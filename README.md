# XSLT as a Service

An XSLT service to be deployed to the [Services-as-a-Service server](https://github.com/danielrendall/ServicesAsAService)

If you have the need to run XSLT 2 transformations on lots of files as part of
some kind of batch process, this may be useful to you. Run the server, deploy
this on some suitable endpoint, deploy XSLT by PUTting your stylesheets (with
optional default parameters) to your choice of endpoints, and then get
transformed XML by POSTing your source XML to those endpoints (with parameters
if necessary) and saving the results.

**This is intended for use on a machine not accessible from the internet. The
server is built around the idea of remote code execution - it should not be made
available to scamps and scallywags who might do naughty things with it**

## Usage

Sketchy initial documentation, to be improved:

Build this, using the sbt "assembly" task to create an "assembled" version of
the JAR, then PUT it to the _service endpoint on your running server like so:

```shell
curl -XPUT --data-binary @the_assembled_jar.jar http://localhost:1810/_service/xslt
```

Now, PUT whatever XSLTs you want to endpoints of your choice (note that you have
mounted this service on `/xslt`):

```shell
curl -XPUT --data-binary @my_xsl.xsl http://localhost:1810/xslt/first
curl -XPUT --data-binary @my_other_xsl.xsl http://localhost:1810/xslt/second
```

NOW you should be able to POST an XML to have it processed:

```shell
curl -XPOST --data-binary @some_xml.xml http://localhost:1810/xslt/first > some_xml_transformed_by_first.xml
```

Note - use the `--http1.0` flag in your `curl` to avoid irritating 1s delays

## Status

* Needs proper documentation
* And also some actual tests
