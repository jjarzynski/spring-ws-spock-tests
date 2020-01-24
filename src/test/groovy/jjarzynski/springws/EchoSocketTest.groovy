package jjarzynski.springws

import com.neovisionaries.ws.client.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = SpringWsApplication.class, webEnvironment = RANDOM_PORT)
class EchoSocketTest extends Specification {

    @LocalServerPort int port

    def "socket opens"() {
        when:
        def socket = new WebSocketFactory()
                .createSocket("http://localhost:${port}/echo")
                .connect()

        then:
        socket.isOpen()

        cleanup:
        socket.disconnect(WebSocketCloseCode.NORMAL, null, 0)
    }

    def "responds with original message (not recommended)"() {
        given:
        def socket = openSocket()

        and:
        def listenerMock = Mock(WebSocketListener)
        socket.addListener(listenerMock)

        when:
        socket.sendText("Hello")
        sleep(100)

        then:
        1 * listenerMock.onTextMessage(_, "Hello")

        cleanup:
        closeSocket(socket)
    }

    def "ignores empty message (not recommended)"() {
        given:
        def socket = openSocket()

        and:
        def listenerMock = Mock(WebSocketListener)
        socket.addListener(listenerMock)

        when:
        socket.sendText("")
        sleep(100)

        then:
        0 * listenerMock.onTextMessage(_, "Hello")

        cleanup:
        closeSocket(socket)
    }

    def "responds with original message"() {
        given:
        def latch = new BlockingVariable<String>(0.5)

        and:
        def socket = openSocket(latch)

        when:
        socket.sendText("Hello")

        then:
        with(latch.get()) {
            it == "Hello"
        }

        cleanup:
        closeSocket(socket)
    }

    def "ignores empty message"() {
        given:
        def latch = new BlockingVariable<String>(0.5)

        and:
        def socket = new WebSocketFactory()
                .createSocket("http://localhost:${port}/echo")
                .connect()
                .addListener(new WebSocketAdapter() {
                    @Override
                    void onTextMessage(WebSocket websocket, String text) throws Exception {
                        latch.set(text)
                    }
                })

        when:
        socket.sendText("")
        socket.sendText("Hello")

        then:
        with(latch.get()) {
            it == "Hello"
        }

        cleanup:
        closeSocket(socket)
    }

    def openSocket() {
        new WebSocketFactory()
                .createSocket("http://localhost:${port}/echo")
                .connect()
    }

    def openSocket(latch) {
        openSocket().addListener(new WebSocketAdapter() {
            @Override
            void onTextMessage(WebSocket websocket, String text) throws Exception {
                latch.set(text)
            }
        })
    }

    static def closeSocket(socket) {
        socket.disconnect(WebSocketCloseCode.NORMAL, null, 0)
    }
}
