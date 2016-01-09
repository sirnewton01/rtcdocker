package main

import "fmt"
import "io"
import "log"
import "net/http"
import "os"
import "os/exec"
import "path/filepath"
import "strings"

func main() {
	// Submit the bundles zip as a POST
	http.HandleFunc("/submitbundles", func(w http.ResponseWriter, r *http.Request) {
		os.Remove("/tmp/patch.zip")
		file, err := os.Create("/tmp/patch.zip")
		if err != nil {
			panic(err)
		}
		defer file.Close()
		_, err = io.Copy(file, r.Body)
		if err != nil {
			w.WriteHeader(500)
			fmt.Printf("Bundles zip err\n")
		}
		fmt.Printf("Bundles zip submitted\n")
	})
	// Patch the server and wait for it to come up again
	http.HandleFunc("/patchserver", func(w http.ResponseWriter, r *http.Request) {
		fmt.Printf("Patching server\n")

		err := os.RemoveAll("/tmp/patch")
		if err != nil {
			panic(err)
		}
		err = os.Mkdir("/tmp/patch", 0700)
		if err != nil {
			panic(err)
		}
		unzip := exec.Cmd {
				Path: "/usr/bin/unzip",
				Args: []string{"/usr/bin/unzip", "/tmp/patch.zip"},
				Dir: "/tmp/patch",
				Stdout: os.Stdout,
				Stderr: os.Stderr,
		}
		err = unzip.Run()
		if err != nil {
			w.WriteHeader(400)
			fmt.Printf("Unable to unzip bundles zip")
			return
		}

		os.Remove("/tmp/patch.zip")

		patcherSitePath := "/ccmserver/server/conf/ccm/sites/patcher-site/plugins"
		os.MkdirAll(patcherSitePath, 0700)
		_, err = os.Stat(patcherSitePath)
		if err != nil {
			panic(err)
		}

		filepath.Walk("/tmp/patch", func(bundlePath string, info os.FileInfo, err error) error {
			if strings.HasSuffix(bundlePath, ".jar") {
				bundleId := strings.Split(filepath.Base(bundlePath), "_")[0]

				found := false

				filepath.Walk("/ccmserver/server/conf/ccm/sites", func (targetBundlePath string, info os.FileInfo, err error) error {
					if strings.HasSuffix(targetBundlePath, ".jar") && strings.HasPrefix(filepath.Base(targetBundlePath), bundleId + "_") {
						dest, err := os.Create(targetBundlePath)
						if err != nil {
							panic(err)
						}
						defer dest.Close()
						src, err := os.Open(bundlePath)
						if err != nil {
							panic(err)
						}
						defer src.Close()
						_, err = io.Copy(dest, src)
						if err != nil {
							panic(err)
						}

						fmt.Printf("%v has been patched\n", targetBundlePath)
						found = true
					}

					return nil
				})

				if !found {
					dest, err := os.Create(filepath.Join(patcherSitePath, filepath.Base(bundlePath)))
					if err != nil {
						panic(err)
					}
					defer dest.Close()
					src, err := os.Open(bundlePath)
					if err != nil {
						panic(err)
					}
					defer src.Close()

					_, err = io.Copy(dest, src)
					if err != nil {
						panic(err)
					}

					fmt.Printf("%v has been copied to the patcher site in %v\n", bundlePath, patcherSitePath)
				}
			}

			return nil
		})

		siteXml, err := os.Create(filepath.Join(patcherSitePath, "../site.xml"))
		if err != nil {
			panic(err)
		}
		defer siteXml.Close()

		siteXml.Write([]byte(`<?xml version="1.0" encoding="UTF-8"?>
<site>
<description>
        Patch   
</description>
<feature id="patch" url="features/patch_1.0.100.v20150225_2000.jar" version=
"1.0.100_v20150225_2000" />
</site>

`))


		err = os.MkdirAll(filepath.Join(patcherSitePath, "../features"), 0700)
		if err != nil {
			panic(err)
		}

		featureXml, err := os.Create(filepath.Join(patcherSitePath, "../features/feature.xml"))
		if err != nil {
			panic(err)
		}
	
		featureXml.Write([]byte(`
<feature
      id="patch"
      label="Patch"      
      version="1.0.100.v20150525_2000"
      provider-name="Patcher">

   <description>
   </description>

   <copyright>
   </copyright>

   <license>
   </license>

   <requires>
   </requires>
`))


		filepath.Walk(patcherSitePath, func (bundlePath string, info os.FileInfo, e error) error {
			if strings.HasSuffix(bundlePath, ".jar") {
				bundleId := strings.Split(filepath.Base(bundlePath), "_")[0]
				bundleVersion := strings.Join(strings.Split(filepath.Base(bundlePath), "_")[1:], "_")
				bundleVersion = strings.Replace(bundleVersion, ".jar", "", 1)
				featureXml.Write([]byte(fmt.Sprintf(`
<plugin
         id="%v"
         download-size="0"
         install-size="0"
         version="%v"/>

`, bundleId, bundleVersion)))
			}

			return nil
		})

		featureXml.Write([]byte(`
</feature>
`))

		featureXml.Close()

                zip := exec.Cmd {
                                Path: "/usr/bin/zip",
                                Args: []string{"/usr/bin/zip", "patch_1.0.100.v20150225_2000.jar", "feature.xml"},
                                Dir: filepath.Join(patcherSitePath, "../features"),
                                Stdout: os.Stdout,
                                Stderr: os.Stderr,
                }
		err = zip.Run()
		if err != nil {
			panic(err)
		}

		os.Remove(filepath.Join(patcherSitePath, "../features/feature.xml"))

		siteIni, err := os.Create("/ccmserver/server/conf/ccm/provision_profiles/patcher-site.ini")
		if err != nil {
			panic(err)
		}
		defer siteIni.Close()
		siteIni.Write([]byte(`url=file:ccm/sites/patcher-site
featureid=patch`))

		client := authenticate("https://localhost:9443/ccm", "TestJazzAdmin1", "TestJazzAdmin1")
		client.Get("https://localhost:9443/ccm/admin/cmd/requestReset")

		restart, err := os.Create("/restart.sh")
		if err != nil {
			panic(err)
		}
		restart.Write([]byte(`#!/bin/sh
/ccmserver/server/repotools-ccm.sh -clean -addTables noPrompt
`))
		restart.Close()

		exec.Command("chmod", "u+x", "/restart.sh").Run()
		exec.Command("/docker-shutdown.sh").Run()
		// Get rid of any hot swapped in web resources in case these new bundles contain the newest versions
		os.RemoveAll("/swap")
	})
        // Swap in a new version of a particular web resource
        http.HandleFunc("/swapresource/", func(w http.ResponseWriter, r *http.Request) {
		resourcePath := "/" + strings.Join(strings.Split(r.URL.Path, "/")[2:], "/")
		swapPath := filepath.Join("/swap", resourcePath)
                fmt.Printf("Swapping web resource %v in %v\n", resourcePath, swapPath)
		os.MkdirAll(filepath.Dir(swapPath), 0777)
		swapFile, err := os.Create(swapPath)
		if err != nil {
			panic(err)
		}
		defer swapFile.Close()
		_, err = io.Copy(swapFile, r.Body)
		if err != nil {
			panic(err)
		}
        })

	fmt.Printf("Server manager listening on port 9000\n")

	log.Fatal(http.ListenAndServe(":9000", nil))
}
