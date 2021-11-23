package com.example.primenumber

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import akka.pki.pem.{DERPrivateKeyLoader, PEMDecoder}
import com.typesafe.config.Config

import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.io.Source

/**
 * Adds support for HTTPS to services
 */
trait WithHttps {

  protected def serverHttpContext(config: Config, pemLocation: String, keyLocation: String): HttpsConnectionContext = {
    val privateKey = DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem(config, keyLocation)))
    val fact = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(
      classOf[PrimeNumberServer].getResourceAsStream(config.getString(pemLocation))
    )
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry(
      "private",
      privateKey,
      new Array[Char](0),
      Array[Certificate](cer)
    )
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    ConnectionContext.httpsServer(context)
  }

  protected  def readPrivateKeyPem(config: Config, keyLocation: String): String =
    Source.fromResource(config.getString(keyLocation)).mkString
}
