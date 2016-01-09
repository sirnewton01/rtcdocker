package main

import (
	"crypto/tls"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"strings"
)

func authenticate(baseUrl string, userId string, password string) *http.Client {
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	cookieJar, _ := cookiejar.New(nil)
	client := &http.Client{Transport: tr, Jar: cookieJar}

	sessionReq, err := http.NewRequest("GET", baseUrl+"/oslc/workitems/1.xml", nil)
	if err != nil {
		panic(err)
	}
	resp, err := client.Do(sessionReq)
	if err != nil {
		panic(err)
	}
	resp.Body.Close()

	identityReq, err := http.NewRequest("GET", baseUrl+"/authenticated/identity", nil)
	if err != nil {
		panic(err)
	}
	resp, err = client.Do(identityReq)
	if err != nil {
		panic(err)
	}

	form := url.Values{}
	form.Add("j_username", userId)
	form.Add("j_password", password)

	authReq, err := http.NewRequest("POST", baseUrl+"/j_security_check", strings.NewReader(form.Encode()))
	if err != nil {
		panic(err)
	}
	authReq.Header.Add("Content-Type", "application/x-www-form-urlencoded")

	resp, err = client.Do(authReq)
	if err != nil {
		panic(err)
	}

	resp.Body.Close()

	return client
}

