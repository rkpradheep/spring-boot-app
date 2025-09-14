forceHttpsRedirect()

(function() {

    var video = document.querySelector('video');
    var canvas = document.querySelector('canvas');
    var img = document.querySelector('img');
    var context = canvas.getContext('2d');
    var socket;
    var url = "ws://";
    if (window.location.origin.includes('https'))
        url = "wss://";
    url = url + window.location.host + "/api/v1/broadcast";
    socket = new WebSocket(url);

    socket.addEventListener('open', function(event) {
        setInterval(main, 1000);
    });
    var constraints = {
        video: true,
        audio: false
    };

    navigator.mediaDevices.getUserMedia(constraints).then(function(stream) {
        video.srcObject = stream;
        video.play();
    }).catch(function(err) {
        console.log(err)
    });

    function main() {
        drawCanvas();
        readCanvas();
    }

    function drawCanvas() {
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
    }

    function readCanvas() {
        if (socket.readyState === WebSocket.OPEN) {
            canvas.toBlob(function(blob) {
                socket.send(blob);
            }, 'image/jpeg', 1);
        }
    }
})();