package com.cgutman.adblib;
import java.io.*;
import java.nio.*;

public final class AdbProtocolTest {
    private static void reject(byte[] bytes) throws Exception {
        try { AdbProtocol.AdbMessage.parseAdbMessage(new ByteArrayInputStream(bytes)); throw new AssertionError("Accepted invalid header"); }
        catch (IOException expected) {}
    }
    @SuppressWarnings("resource") // Queue-only streams use no socket; notifyClose is tested directly.
    public static void main(String[] args) throws Exception {
        for (int size : new int[]{-1,4097,Integer.MAX_VALUE}) {
            byte[] message = AdbProtocol.generateReady(1,2);
            ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN).putInt(12,size);
            reject(message);
        }
        byte[] badMagic = AdbProtocol.generateReady(1,2); badMagic[20] ^= 1; reject(badMagic);
        reject(new byte[12]);
        byte[] badChecksum = AdbProtocol.generateWrite(1,2,new byte[]{5}); badChecksum[24] ^= 1;
        if (AdbProtocol.validateMessage(AdbProtocol.AdbMessage.parseAdbMessage(new ByteArrayInputStream(badChecksum)))) throw new AssertionError();
        byte[] utf8 = AdbProtocol.generateOpen(1,"shell:echo 菜单");
        if (!AdbProtocol.validateMessage(AdbProtocol.AdbMessage.parseAdbMessage(new ByteArrayInputStream(utf8)))) throw new AssertionError();
        AdbStream stream = new AdbStream(null,1);
        for (int i=0;i<64;i++) stream.addPayload(new byte[4096]);
        try { stream.addPayload(new byte[1]); throw new AssertionError("Unbounded queue"); } catch (IOException expected) {}
        stream.read(); stream.addPayload(new byte[4096]);
        stream.notifyClose(); if (stream.wasClosedByPeer()) throw new AssertionError();
        AdbStream peerClosed = new AdbStream(null,2); peerClosed.notifyPeerClose();
        if (!peerClosed.wasClosedByPeer()) throw new AssertionError();
        System.out.println("PASS: packet bounds, magic/checksum, UTF-8 destinations, bounded queues, close reasons");
    }
}
