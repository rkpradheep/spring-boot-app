var socket;
var live = false
const video = document.getElementById('streamVideo');

const mimeCodec = 'video/webm;codecs=vp8,opus';
var mediaSource;
video.onloadedmetadata = (event) => {
    console.log("Metadata loaded");
    video.play()
    //console.log(event)
};
var sourceBuffer;

function startLive() {
    if (live) {
        alert("You are already on live")
        return
    }

    var url = "ws://";
    if (window.location.origin.includes('https'))
        url = "wss://";

    url = url + window.location.host + "/api/v1/live";
    socket = new WebSocket(url)
    socket.addEventListener("error", (event) => {
        console.log("WebSocket closed: " + event);
        live = false;
    })
    setupMedia();
}

function sourceOpen() {
    live = true;
    sourceBuffer = mediaSource.addSourceBuffer(mimeCodec);
    sourceBuffer.mode = "sequence";
    socket.onmessage = (buf) => {
        if (buf.data == "reset") {
            console.log("Signal received to reset")
            setupMedia();
            return;
        }
        const reader = new FileReader();
        reader.onload = function() {
            //console.log("Video src")
            //console.log(reader.result)
            if (!sourceBuffer.updating) {
                sourceBuffer.appendBuffer(reader.result);
            } else {
                // Wait for the updating state to become false
                sourceBuffer.addEventListener('updateend', function() {
                    sourceBuffer.appendBuffer(reader.result);
                });
            }
        };
        reader.readAsArrayBuffer(buf.data);
    }
}

function stopLive() {
    console.log("Live stopped")
    live = false;
    socket.close()
}

function setupMedia() {
    console.log("Setting up process started")
    mediaSource = new MediaSource()
    mediaSource.addEventListener('sourceopen', function() {
        sourceOpen()
    });
    video.src = URL.createObjectURL(mediaSource);
}