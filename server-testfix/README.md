This iamge takes an existing GA/iFix image and patches the plugins with the
provided jars in a similar way that hot fixes or temporary test fixes are
given to customers. Put the patched JAR files in the server-patches directory,
modify the FROM clause in the Dockerfile to match the GA/iFix version to be
patched and run the docker build command.
