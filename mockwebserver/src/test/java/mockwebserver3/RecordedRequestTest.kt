/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mockwebserver3

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.net.InetAddress
import java.net.Socket
import mockwebserver3.internal.DEFAULT_REQUEST_LINE_HTTP_1
import mockwebserver3.internal.MockWebServerSocket
import mockwebserver3.internal.RecordedRequest
import mockwebserver3.internal.decodeRequestLine
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okio.ByteString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(30)
class RecordedRequestTest {
  private val headers: Headers = Headers.EMPTY

  @Test fun testIPv4() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress = InetAddress.getByAddress("127.0.0.1", byteArrayOf(127, 0, 0, 1)),
          localPort = 80,
        ),
      )
    val request = RecordedRequest(DEFAULT_REQUEST_LINE_HTTP_1, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.url.toString()).isEqualTo("http://127.0.0.1/")
  }

  @Test fun testAuthorityForm() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress = InetAddress.getByAddress("127.0.0.1", byteArrayOf(127, 0, 0, 1)),
          localPort = 80,
        ),
      )
    val requestLine = decodeRequestLine("CONNECT example.com:8080 HTTP/1.1")
    val request = RecordedRequest(requestLine, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.target).isEqualTo("example.com:8080")
    assertThat(request.url.toString()).isEqualTo("http://example.com:8080/")
  }

  @Test fun testAbsoluteForm() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress = InetAddress.getByAddress("127.0.0.1", byteArrayOf(127, 0, 0, 1)),
          localPort = 80,
        ),
      )
    val requestLine = decodeRequestLine("GET http://example.com:8080/index.html HTTP/1.1")
    val request = RecordedRequest(requestLine, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.target).isEqualTo("http://example.com:8080/index.html")
    assertThat(request.url.toString()).isEqualTo("http://example.com:8080/index.html")
  }

  @Test fun testAsteriskForm() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress = InetAddress.getByAddress("127.0.0.1", byteArrayOf(127, 0, 0, 1)),
          localPort = 80,
        ),
      )
    val requestLine = decodeRequestLine("OPTIONS * HTTP/1.1")
    val request =
      RecordedRequest(
        requestLine,
        headers,
        emptyList(),
        0,
        ByteString.EMPTY,
        0,
        0,
        socket,
      )
    assertThat(request.target).isEqualTo("*")
    assertThat(request.url.toString()).isEqualTo("http://127.0.0.1/")
  }

  @Test fun testIpv6() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress =
            InetAddress.getByAddress(
              "::1",
              byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            ),
          localPort = 80,
        ),
      )
    val request = RecordedRequest(DEFAULT_REQUEST_LINE_HTTP_1, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.url.toString()).isEqualTo("http://[::1]/")
  }

  @Test fun testUsesLocal() {
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress = InetAddress.getByAddress("127.0.0.1", byteArrayOf(127, 0, 0, 1)),
          localPort = 80,
        ),
      )
    val request = RecordedRequest(DEFAULT_REQUEST_LINE_HTTP_1, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.url.toString()).isEqualTo("http://127.0.0.1/")
  }

  @Test fun testHostname() {
    val headers = headersOf("Host", "host-from-header.com")
    val socket =
      MockWebServerSocket(
        FakeSocket(
          localAddress =
            InetAddress.getByAddress(
              "host-from-address.com",
              byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            ),
          localPort = 80,
        ),
      )
    val request = RecordedRequest(DEFAULT_REQUEST_LINE_HTTP_1, headers, emptyList(), 0, ByteString.EMPTY, 0, 0, socket)
    assertThat(request.url.toString()).isEqualTo("http://host-from-header.com/")
  }

  private class FakeSocket(
    private val localAddress: InetAddress,
    private val localPort: Int,
    private val remoteAddress: InetAddress = localAddress,
    private val remotePort: Int = 1234,
  ) : Socket() {
    override fun getInetAddress() = remoteAddress

    override fun getLocalAddress() = localAddress

    override fun getLocalPort() = localPort

    override fun getPort() = remotePort
  }
}
