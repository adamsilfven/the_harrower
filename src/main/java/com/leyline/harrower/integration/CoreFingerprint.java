package com.leyline.harrower.integration;
import java.security.*;
import java.io.*;
import java.util.*;

/** Audited original bytecode fingerprints. Unknown Core builds receive no injections. */
public final class CoreFingerprint {
 private static final Map<String,String> EXPECTED=Map.of(
  "com/leylineconvergence/core/endgame/EndgameService.class","953f12a66de3122bf18642c5fdc74e3203495eb9b6efaeedfd8f5bce4039148c",
  "com/leylineconvergence/core/land/LandRules$Binding.class","e36eb5bcc9f1820520171d6842c2b2681599720fec443dd6659339822d9df8aa",
  "com/leylineconvergence/core/land/LandService.class","9f1a6717afd6b71edc6d3f43bb24e9e40d2b7c15da8ec8e828909dc8cbe5fe76");
 public static boolean matches(){
  ClassLoader loader=Thread.currentThread().getContextClassLoader();
  try{for(var entry:EXPECTED.entrySet())try(InputStream in=loader.getResourceAsStream(entry.getKey())){if(in==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())).equals(entry.getValue()))return false;}return true;}catch(Exception exception){return false;}
 }
 private CoreFingerprint(){}
}
