package com.cgutman.adblib;
import java.net.*;import java.io.*;
public class AdbSmokeTest {
 public static void main(String[] args)throws Exception {
  for(int i=0;i<60;i++){
   ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
   java.util.concurrent.atomic.AtomicReference<Throwable> peerFailure=new java.util.concurrent.atomic.AtomicReference<>();
   server.setSoTimeout(5000);
   Thread peer=new Thread(()->{
    try(Socket s=server.accept()){
     s.setSoTimeout(5000);InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();
     AdbProtocol.AdbMessage.parseAdbMessage(in);out.write(AdbProtocol.generateConnect());out.flush();
     AdbProtocol.AdbMessage open=AdbProtocol.AdbMessage.parseAdbMessage(in);
     out.write(AdbProtocol.generateReady(42,open.arg0));
     out.write(AdbProtocol.generateWrite(42,open.arg0,"PASS\n".getBytes("UTF-8")));
     out.write(AdbProtocol.generateClose(42,open.arg0));out.flush();
     AdbProtocol.AdbMessage.parseAdbMessage(in);
    }catch(Exception e){peerFailure.set(e);}
   });peer.setDaemon(true);peer.start();
   try(AdbConnection c=AdbConnection.create(new Socket(InetAddress.getLoopbackAddress(),server.getLocalPort()),null)){
    c.connect();try(AdbStream stream=c.open("shell:echo PASS")){
     ByteArrayOutputStream b=new ByteArrayOutputStream();
     while(true){try{b.write(stream.read());}catch(IOException closed){break;}}
     if(!b.toString("UTF-8").equals("PASS\n"))throw new AssertionError("Lost final payload: "+b);
    }
   }
   peer.join(6000);server.close();
   if(peer.isAlive())throw new AssertionError("Mock ADB peer did not finish");
   if(peerFailure.get()!=null)throw new AssertionError("Mock ADB peer failed",peerFailure.get());
  }
  System.out.println("PASS: 60 immediate ADB OPEN/WRTE/CLSE cycles; final payload retained; no lost wakeup");
 }
}
