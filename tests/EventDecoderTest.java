import local.sony.keyfix.EventDecoder;
import java.io.*;import java.nio.*;import java.util.*;
public class EventDecoderTest {
 static byte[] e(int t,int c,int v){return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(2).putShort((short)t).putShort((short)c).putInt(v).array();}
 static void event(ByteArrayOutputStream b,int t,int c,int v)throws Exception{b.write(e(t,c,v));}
 public static void main(String[] args)throws Exception{
  List<Integer> expected=Arrays.asList(0xc0533,0xc03c3,0xc051f,0xc038b); ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  for(int usage:expected){
   event(bytes,4,4,usage);event(bytes,1,240,1);event(bytes,0,0,0);
   event(bytes,4,4,usage);event(bytes,1,240,2);event(bytes,0,0,0);
   event(bytes,4,4,usage);event(bytes,1,240,1);event(bytes,0,0,0);
   event(bytes,4,4,usage);event(bytes,1,240,0);event(bytes,0,0,0);
  }
  // A known navigation key and a keydown without MSC_SCAN must not trigger.
  event(bytes,4,4,0xc0533);event(bytes,1,158,1);event(bytes,0,0,0);event(bytes,1,240,1);
  byte[] data=bytes.toByteArray();
  for(int chunk=1;chunk<=79;chunk++){
   List<Integer> got=new ArrayList<>();EventDecoder d=new EventDecoder(got::add);
   for(int pos=0;pos<data.length;pos+=chunk)d.accept(Arrays.copyOfRange(data,pos,Math.min(data.length,pos+chunk)));
   if(!got.equals(expected))throw new AssertionError(chunk+" "+got);
  }
  List<Integer> recovered=new ArrayList<>(); EventDecoder recovery=new EventDecoder(recovered::add);
  ByteArrayOutputStream overflow=new ByteArrayOutputStream();
  event(overflow,0,3,0); event(overflow,4,4,0xc0533); event(overflow,1,240,1); event(overflow,0,0,0);
  event(overflow,4,4,0xc03c3); event(overflow,1,240,1); event(overflow,0,0,0);
  recovery.accept(overflow.toByteArray());
  if(!recovered.equals(Arrays.asList(0xc03c3)))throw new AssertionError("Overflow recovery: "+recovered);
  System.out.println("PASS: all four scans, 79 packet fragment sizes, keyup/repeat/duplicate suppression, ordinary keys, SYN_DROPPED");
 }
}
