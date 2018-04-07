# NettyBug
Potential race condition in Netty library when proxying to internal channels. Version 4.1.23

### Overview
I am new to Netty.

I built a HTTP1.1 proxy server modeling the implementation after the HexDumpProxy example from the official Netty examples. The strategy used was to use AUTO_READ=false and have the handlers read and write 1 message at a time to solve for back pressure.

In the simple case, everything works fine. 

I then introduced a LocalServerChannel in the middle. The first handler proxies the request to the LocalServerChannel which in turn proxies the request to the real destination. All handlers use the same strategy for back pressure (read/write 1 at a time). Each handler writes requests to the next channel and writes responses to the previous. This version suffers from an intermittent freeze.

### Expected behavior
I expected the proxy with the LocalServerChannel to work just as well as the example without the local server.

### Actual behavior
The LocalServerChannel version suffers from an intermittent freeze. It looks like the data is written but the read() function isn't called on the handler.

If i turn AUTO_READ=true this issue goes away

### Steps to reproduce
go to https://github.com/bradforj287/NettyBug and clone repo

observe files in package:

1. ServerMainBroken - this server will exhibit the problem. 
2. ServerMainWorking - this server works fine. Does not use local channel
3. ServerMainWorking2 - this server is identical to ServerMainBroken except it uses AUTO_READ=true

Steps to reproduce:
1. inspect ServerMainBroken. Notice the proxy destination is hard coded to localhost/5000. Change that to another site. I verified www.w3schools.com/443 will demonstrate this issue. All sites should expose issue.
2. launch ServerMainBroken
3. issue a few curl commands to the server. You should notice the issue if you continue to spam curls many times. IT seems as if the larger payloads (e.g. long HTML pages, etc) expose the issue at a higher rate. Go to next step for more automated way to test this.
4. Open TestClient. Change the target URL to match the ServerMainBroken server. E.g. use http://localhost:9090/<path>
5. Run TestClient and observe after some time it will get stuck. Sometimes when it gets stuck it gets unstuck after a long time. It either freezes completely or is extremely slow.
6. Now repeat these steps for ServerMainWorking and ServerMainWorking2 and observe that they work well. Against a locally running server, I have verified that the TestClient can execute 1M + GET requests against ServerMainWorking or ServerMainWorking2 with no issue whatsoever. ServerMainBroken dies every time.

### Minimal yet complete reproducer code (or URL to code)
https://github.com/bradforj287/NettyBug 

### Netty version
4.1.23.Final

### JVM version (e.g. `java -version`)
java version "1.8.0_112"
Java(TM) SE Runtime Environment (build 1.8.0_112-b16)
Java HotSpot(TM) 64-Bit Server VM (build 25.112-b16, mixed mode)

### OS version (e.g. `uname -a`)
MacOS Sierra 10.12.6 
