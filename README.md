# ffvaapi
   This is a small GUI frontend for FFmpeg that I made to make converting videos to X.264 and X.265 easier. This app will ONLY WORK IN LINUX! It uses VAAPI to decode and encode, so you need a GPU that supports VAAPI and the VAAPI software drivers for that GPU. Also you need to install "vainfo", apt-get install vainfo for Debian/Ubuntu. Vainfo can tell you if your VAAPI software is involved and what you GPU can encode and decode using hardware. It's also used by my app so have installed. FFmpeg must be installed too, of course. This should work with INTEL, AMD, and NVIDIA GPUs.
   
   This app was tested with:
   -Ubuntu 19.10 and 20.04
   -Radeon RX570 4gb GPU (mesa drivers)
   -AMD FX 8320E CPU
   
   
Installation:

  Just download the JAR file to a desirable place on your hardrive and run. Make sure you have installed: ffmpeg, vainfo, and GPU drivers with VAAPI. Run "vainfo" in a terminal to check and see what your GPU can encode and decode with. Look for "VAProfileH264****" and "VAProfileHEVC****", then look to the right of them and the ones that say "VAEntrypointEncSlice" mean it can encode that and "VAEntrypointVLD" means it can decode that. Remember HVEC is X.265. I made the app check for encoders and disable H.265 if not found, but there's no check for decoders. It assumes your GPU can do both H.264 and H.265 (HVEC) decoding. This app can only use hardware decoding and encoding. It cannot do software decoding or encoding. There are several apps that can do that with FFmpeg already. I uploaded a desktop file too, so you can have the app on you app menu. Edit FF-VAAPI.desktop file at the end with the installion path if the FF-VAAPI.jar file on your hard drive. When FF-VAAPI.desktop is updated, move it to /usr/share/applications/
  
  
Features:

 -converts videos to H.264 or H.265 (HVEC)
 -remove audio
 -set video resolution
 -set bitrate
 -set FPS

Download JAR file and a little more info on it:
http://www.swampsoft.org/java/ffvaapi/ffvaapi.php
