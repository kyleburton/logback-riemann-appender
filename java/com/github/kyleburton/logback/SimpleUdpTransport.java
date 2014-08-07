// From: https://github.com/aphyr/riemann-java-client/pull/24
package com.github.kyleburton.logback;

import com.aphyr.riemann.client.SynchronousTransport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SimpleUdpTransport implements SynchronousTransport {

  public static final int DEFAULT_PORT = 5555;

  private volatile DatagramSocket socket;

  private final InetSocketAddress address;

  public SimpleUdpTransport(final InetSocketAddress address) {
    this.address = address;
  }

  public SimpleUdpTransport(final String host, final int port) throws IOException {
    this(new InetSocketAddress(host, port));
  }

  public SimpleUdpTransport(final String host) throws IOException {
    this(host, DEFAULT_PORT);
  }

  public SimpleUdpTransport(final int port) throws IOException {
    this(InetAddress.getLocalHost().getHostAddress(), port);
  }

  @Override
  public com.aphyr.riemann.Proto.Msg sendMaybeRecvMessage(final com.aphyr.riemann.Proto.Msg msg) throws IOException {
    final byte[] body = msg.toByteArray();
    final DatagramPacket packet = new DatagramPacket(body, body.length, address);
    socket.send(packet);
    return null;
  }

  @Override
  public com.aphyr.riemann.Proto.Msg sendRecvMessage(final com.aphyr.riemann.Proto.Msg msg) throws IOException {
    throw new UnsupportedOperationException("UDP transport doesn't support receiving messages");
  }

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public synchronized void connect() throws IOException {
    socket = new DatagramSocket();
  }

  @Override
  public synchronized void disconnect() throws IOException {
    try {
      socket.close();
    } finally {
      socket = null;
    }
  }

  @Override
  public void reconnect() throws IOException {
    disconnect();
    connect();
  }

  @Override
  public void flush() throws IOException {}
}
