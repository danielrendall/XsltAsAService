package uk.co.danielrendall.saas.xslt

import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.Status
import net.sf.saxon.TransformerFactoryImpl
import uk.co.danielrendall.saas.interfaces.*
import uk.co.danielrendall.saas.xslt.Exceptions.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.math.BigInteger
import java.security.MessageDigest
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.transform.{Templates, TransformerFactory}
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class XsltService
  extends Serviceable
  with ResponseHelpers:

  private lazy val tf: TransformerFactory =  new TransformerFactoryImpl()

  /**
   * Map of user-visible name of XSLT to the MD5 hash of that XSLT; so if we have the same XSLT bound to a number of
   * different names, we don't waste our time compiling them.
   */
  private val nameToHash: mutable.Map[String, String] =
    new mutable.HashMap[String, String]()

  /**
   * Map of MD5 hash of an XSLT to the compiled Templates for that XSLT
   */
  private val hashToTemplates: mutable.Map[String, Templates] =
    new mutable.HashMap[String, Templates]()

  /**
   * Map of user-visible name of XSLT to and default parameters which should always be passed to that XSLT (which can
   * be added to via query-string params)
   */
  private val nameToDefaultParams: mutable.Map[String, Map[String, String]] =
    new mutable.HashMap[String, Map[String, String]]()

  def getMetadata: ServiceMetadata = ServiceMetadata("XSLT")


  def get(session: ServiceSession, first: String, rest: List[String])
         (implicit responseFactory: ResponseFactory): Response =
    badRequest("GET doesn't do anything")


  def post(session: ServiceSession, first: String, rest: List[String])
          (implicit responseFactory: ResponseFactory): Response =
    (for {
      bytes <- Try(session.bodyAsBytes)
      _ <- if (bytes.nonEmpty) Success(()) else Failure(NoDocumentSuppliedException)
      result <- process(first, bytes, session.reducedQueryParameters)
    } yield result) match {
    case Success(result) =>
      responseFactory.newFixedLengthResponse(Status.OK, "text/xml", new ByteArrayInputStream(result), result.length)
    case Failure(ex) =>
      badRequest(ex.getMessage)
    }

  def put(session: ServiceSession, first: String, rest: List[String])
         (implicit responseFactory: ResponseFactory): Response =
    (for {
      bytes <- Try(session.bodyAsBytes)
      _ <- if (bytes.nonEmpty) saveTemplates(first, bytes) else Success(())
      _ <- saveParams(first, session.reducedQueryParameters)
    } yield ()) match {
      case Failure(ex) =>
        ex.printStackTrace()
        badRequest(ex.getMessage)
      case Success(_) =>
        ok(s"Put $first")
    }


  def delete(session: ServiceSession, first: String, rest: List[String])
            (implicit responseFactory: ResponseFactory): Response =
    badRequest("DELETE doesn't do anything")



  private def saveTemplates(name: String, code: Array[Byte]): Try[Unit] =
    Try {
      val hash = String.format("%032x", new BigInteger(1, MessageDigest.getInstance("MD5").digest(code)))
      nameToHash.put(name, hash)
      if (!hashToTemplates.contains(hash)) {
        hashToTemplates.put(hash, tf.newTemplates(new StreamSource(new ByteArrayInputStream(code))))
      }
    }

  private def saveParams(name: String, params: Map[String, String]): Try[Unit] =
    Try {
      nameToDefaultParams.put(name, params)
    }

  private def process(name: String, document: Array[Byte], params: Map[String, String]): Try[Array[Byte]] =
    for {
      templates <- getTemplates(name)
      defaults <- getDefaultParameters(name)
      finalParams: Map[String, String] = defaults ++ params
      result <- process(templates, document, finalParams)
    } yield result

  private def process(templates: Templates, document: Array[Byte], params: Map[String, String]): Try[Array[Byte]] =
    Try {
      val baos = new ByteArrayOutputStream()
      val result = new StreamResult(baos)
      val transformer = templates.newTransformer()
      params.foreach { case (name, value) => transformer.setParameter(name, value) }
      transformer.transform(new StreamSource(new ByteArrayInputStream(document)), result)
      baos.toByteArray
    }

  private def getTemplates(name: String): Try[Templates] =
    Try {
      (for {
        hash <- nameToHash.get(name)
        templates <- hashToTemplates.get(hash)
      } yield templates).getOrElse(throw NoTemplatesStoredException(name))
    }

  private def getDefaultParameters(name: String): Try[Map[String, String]] =
    Try {
      nameToDefaultParams.getOrElse(name, Map.empty)
    }
