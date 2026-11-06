package io.github.supermonster003.autojs6.plugin.three.prism.camera

import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.prism.camera.RepositoryFiles.children
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The extra TLS trust anchor of appendix D Q42 = (b) (the update check on Android 7.0, ported from 3-Macaw Speech
 * cf32b5e): `res/raw/isrg_root_x1.der` is exactly the ISRG Root X1 certificate locked in `locks/trust-anchors.lock`
 * (self-signed, the locked subject and expiry), the network security config adds it next to the system store without
 * opening anything else (no cleartext, no domain overrides, no debug overrides, no pins), and the Manifest applies it.
 * zh-CN: 附录 D Q42 = (b) 的额外 TLS 信任锚 (Android 7.0 上的更新检查, 移植自 3-Macaw Speech cf32b5e): `res/raw/isrg_root_x1.der` 正是
 * `locks/trust-anchors.lock` 锁定的 ISRG Root X1 证书 (自签名, 主题与有效期与锁一致), 网络安全配置只把它追加到系统信任库之外, 不放开其他
 * 任何东西 (无明文, 无按域覆盖, 无 debug 覆盖, 无固定公钥), 且 Manifest 引用了该配置.
 */
class TrustAnchorsTest {

    private val lock = LockFiles.read("locks/trust-anchors.lock")
    private val config = RepositoryFiles.xml("app/src/main/res/xml/network_security_config.xml")

    @Test
    fun `the anchor file is exactly the locked ISRG Root X1 certificate`() {
        assertEquals(listOf("isrg-root-x1"), lock.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") })
        assertEquals("app/src/main/res/raw/isrg_root_x1.der", lock["isrg-root-x1.path"])
        assertEquals("https://letsencrypt.org/certs/isrgrootx1.der", lock["isrg-root-x1.url"])
        assertEquals(lock["isrg-root-x1.sha256"], LockFiles.sha256(lock.getValue("isrg-root-x1.path")))
        val certificate = anchor()
        assertEquals(lock["isrg-root-x1.subject"], certificate.subjectX500Principal.name)
        assertEquals("a self-signed root", certificate.subjectX500Principal, certificate.issuerX500Principal)
        certificate.verify(certificate.publicKey)
        assertEquals(lock["isrg-root-x1.notAfter"], DateTimeFormatter.ISO_INSTANT.format(certificate.notAfter.toInstant()))
        assertTrue("the anchor must stay valid for at least another year", certificate.notAfter.toInstant().isAfter(Instant.now().plus(365, ChronoUnit.DAYS)))
        assertTrue("a CA certificate", certificate.basicConstraints >= 0)
    }

    @Test
    fun `the network security config adds the anchor to the system store and opens nothing else`() {
        assertEquals("network-security-config", config.tagName)
        assertEquals("only a base-config", listOf("base-config"), elementChildren(config).map { it.tagName })
        val base = config.child("base-config")
        assertFalse("cleartext stays at the platform default (blocked since targetSdk 28)", base.hasAttribute("cleartextTrafficPermitted"))
        assertEquals("only trust anchors", listOf("trust-anchors"), elementChildren(base).map { it.tagName })
        val certificates = base.child("trust-anchors").children("certificates")
        assertEquals("the system store first, then the locked anchor", listOf("system", "@raw/isrg_root_x1"), certificates.map { it.getAttribute("src") })
        assertTrue("no overridePins", certificates.none { it.hasAttribute("overridePins") })
    }

    @Test
    fun `the manifest applies the config`() {
        val application = RepositoryFiles.xml("app/src/main/AndroidManifest.xml").child("application")
        assertEquals("@xml/network_security_config", application.android("networkSecurityConfig"))
    }

    private fun anchor(): X509Certificate = Files.newInputStream(RepositoryFiles.path(lock.getValue("isrg-root-x1.path"))).use { input ->
        CertificateFactory.getInstance("X.509").generateCertificate(input) as X509Certificate
    }

    private fun elementChildren(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>()
    }
}
