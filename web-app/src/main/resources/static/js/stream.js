
forceHttpsRedirect();

var url = "wss://";
var socket;
var stop = false;
var tmp;
var restarting = false;
var constraints = {
    video: true,
    audio: true
};
var intervalId;
var mediaStream;
var mediaRecorder;

function startStreamNow() {

    var url = "ws://";
    if (window.location.origin.includes('https'))
        url = "wss://";
    url = url + window.location.host + "/api/v1/stream";
    socket = new WebSocket(url);
    socket.addEventListener('open', function(event) {
        console.log("session started")
        navigator.mediaDevices.getUserMedia(constraints).then(stream => {
            mediaStream = stream;
            mediaRecorder = new MediaRecorder(mediaStream);
            mediaRecorder.onstop = () => {
                if (!stop) {
                    socket.send("restarted")
                }
            };
            startStream()
        });
        //mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
        intervalId = setInterval(() => {
            if (stop) {
                clearInterval(intervalId);
                return;
            }
            // mediaRecorder.requestData()
        }, 2000)
    });
    socket.onmessage = (msg) => {
        if (msg.data == "restartACK") {
            restarting = false;
            console.log("Restart ACK received")
            startStream();
        } else {
            restarting = true;
            mediaRecorder.stop()
            console.log("Restarting Stream")
        }
    };
    socket.addEventListener("close", function(event) {
        console.log("WebSocket Closed:");
        console.log(event)
    });

}

function startStream() {
    try {
        mediaRecorder.start(1000);
        mediaRecorder.ondataavailable = event => {
            if (event.data.size > 0 && !restarting && !stop) {
                //console.log("Video bob")
                //console.log(event.data)
                let reader = new FileReader();
                let rawData = new ArrayBuffer();
                reader.loadend = function() {}
                reader.onload = function(e) {
                    // console.log(e.target.result)
                    if (socket.readyState === WebSocket.OPEN)
                        socket.send(e.target.result)
                    else
                        stopStream()
                }
                reader.readAsArrayBuffer(event.data);
            }
        };

    } catch (error) {
        console.error('Error occurred:', error);
    }
}

function stopStream() {
    console.log("Stopping stream")
    stop = true;
    mediaRecorder.stop()
    const tracks = mediaStream.getTracks();
    tracks.forEach(track => {
        console.log(track)
        track.stop();
    });
    if (socket.readyState === WebSocket.OPEN)
        socket.close()
}