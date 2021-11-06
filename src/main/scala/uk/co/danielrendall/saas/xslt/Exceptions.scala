package uk.co.danielrendall.saas.xslt

object Exceptions:

  case object NoDocumentSuppliedException extends Exception("No document supplied")

  case class NoTemplatesStoredException(name: String) extends Exception(s"No templates stored for key '$name'")
