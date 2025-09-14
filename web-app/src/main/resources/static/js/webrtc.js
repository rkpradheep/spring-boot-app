
forceHttpsRedirect()

var selfView = document.getElementById('self-view');
var remoteView = document.getElementById('remote-view');
var localStream;
var remoteStream;
var signallingServer;
var webRtcPeerConnection;
var onCall = false;
var isOfferer = false;
var combinedStream = new MediaStream();
var signallingServerUrl;
var sessionID;
var name;
var mediaConstraints = {
    video: {
        facingMode: 'user',
        width: 1280,
        height: 720
    },
    audio: false
}
var url = "ws://";
if (window.location.origin.includes('https'))
    url = "wss://";

signallingServerUrl = url + window.location.host + "/signal";

const isMobile =
    navigator.userAgent.match(/Android/i) ||
    navigator.userAgent.match(/webOS/i) ||
    navigator.userAgent.match(/iPhone/i) ||
    navigator.userAgent.match(/iPad/i) ||
    navigator.userAgent.match(/iPod/i) ||
    navigator.userAgent.match(/BlackBerry/i) ||
    navigator.userAgent.match(/Windows Phone/i);

async function startCall() {
    name = prompt("Enter your name")
    if (name == null || name.length < 1) {
        toastMessage("Name is mandatory")
        return;
    }
    toastMessage("Please wait. Initiating ...")
    document.getElementById("join").style.display = "none";
    document.getElementById("call").style.display = "none";
    document.getElementById("callid").style.display = "none";
    isOfferer = true;
    await setupSignallingServer(null, null)
    await setupWebRtcConnection()
}

async function joinCall() {
    sessionID = document.getElementById("callid").value;
    if (sessionID.length < 1) {
        toastMessage("Session ID is mandatory");
        return;
    }
    doCommonTask()
    await setupSignallingServer(sessionID, prompt("Enter name : "))
    await setupWebRtcConnection()
}

async function setupWebRtcConnection() {

fetch("/api/v1/webrtc/iceservers", {
  method: "GET"
})
.then(response =>
{
return response.text();
}
).then(data=> {
    // Handle new standardized response format
    var responseData = data;
    if (data.success !== undefined) {
        responseData = data.data || data;
    }
    webRtcPeerConnection = new RTCPeerConnection(JSON.parse(responseData))
    webRtcPeerConnection.ontrack = event => {
        console.log("Tracks received")
        if (event.track.kind === 'audio') {
            var audioTracks = combinedStream.getAudioTracks();
            if (audioTracks.length > 0) {
                combinedStream.removeTrack(audioTracks[0]);
            }
            combinedStream.addTrack(event.track);
            if (remoteView.srcObject != combinedStream)
                remoteView.srcObject = combinedStream;
        } else if (event.track.kind === 'video') {
            var videoTracks = combinedStream.getVideoTracks();
            if (videoTracks.length > 0) {
                combinedStream.removeTrack(videoTracks[0]);
            }
            combinedStream.addTrack(event.track);
            if (remoteView.srcObject != combinedStream)
                remoteView.srcObject = combinedStream;
        }
        if (remoteView.paused)
            remoteView.play()
    };

    webRtcPeerConnection.onremovetrack = event => {
        console.log("Remote track removed");
        remoteView.srcObject = null;
    }

    webRtcPeerConnection.onicecandidate = function(event) {
        if (!event || !event.candidate) return;
        console.log("Candidate received")
        signallingServer.send(JSON.stringify({
            type: 'candidate',
            candidate: event.candidate,
            id: sessionID,
            is_offerer: isOfferer
        }));
    };
}).catch(error => {
   console.log(error)
});
}

async function setupSignallingServer(id, name) {

    if (signallingServerUrl == null || signallingServerUrl == undefined || signallingServerUrl.length == 0) {
        document.getElementById("join").style.display = "block";
        document.getElementById("call").style.display = "block";
        document.getElementById("callid").style.display = "block";
        toastMessage("Something went wrong. Please try again later")
        return;
    }

    if (id != null)
        signallingServer = new WebSocket(signallingServerUrl + "?id=" + id + "&name=" + name);
    else
        signallingServer = new WebSocket(signallingServerUrl)

    signallingServer.onopen = function() {
        if (!isOfferer) {
            signallingServer.send(JSON.stringify({
                type: 'need_offer',
                id: sessionID,
            }));
        }
        toastMessage("Joined Successfully")
    }

    signallingServer.onclose = function(event) {
        console.log("Connection closed")
        console.log(event.reason)
        if (event.reason == "PeerClosed" && onCall) {
            toastMessage("Call closed by peer")
        }
        if (event.reason == "Already one peer connected")
            toastMessage("Some user have accepted the call already")

        if (event.reason == "InvalidCallId") {
            toastMessage("Invalid Session ID")
        }
        closeCall(false)
    };

    signallingServer.onmessage = function(event) {
        var data = JSON.parse(event.data);
        if (data.type == "joined") {
            document.getElementById("shareid").style.display = "none";
            toastMessage(data.message)
        } else if (data.type == "stream_closed" && remoteView.srcObject != null) {
            var audioTracks = remoteView.srcObject.getAudioTracks()
            var videoTracks = remoteView.srcObject.getVideoTracks()
            if (data.kind == "video" && videoTracks.length > 0) {
                combinedStream = new MediaStream()
                remoteView.srcObject = combinedStream
                if (audioTracks.length > 0) {
                    combinedStream.addTrack(audioTracks[0])
                }
            } else if (audioTracks.length > 0) {
                remoteView.srcObject.removeTrack(audioTracks[0]);
            }
        } else if (data.type == "ACK") {
            toastMessage("Session created successfully")
            console.log("session id")
            console.log(data.id)
            sessionID = data.id
            document.getElementById("callid").value = "Session ID : " + data.id
            document.getElementById("callid").style.disabled = true;
            document.getElementById("shareid").style.display = "block";
            doCommonTask()
        } else if (data.type === 'offer') {
            onCall = true;
            console.log("offer received")
            //combinedStream = new MediaStream();
            webRtcPeerConnection.setRemoteDescription(new RTCSessionDescription(data.offer, onSdpSuccess, onSdpError));
            createAnswer()
        } else if (data.type === 'answer') {
            onCall = true;
            console.log("answer received")
            webRtcPeerConnection.setRemoteDescription(new RTCSessionDescription(data.answer), onSdpSuccess, onSdpError);
        } else if (data.type === 'candidate') {
            console.log("candidate received from peer")
            webRtcPeerConnection.addIceCandidate(new RTCIceCandidate(data.candidate));
        }
    };
}

function onSdpError(e) {
    console.error('SDP failed', e);
}

function onSdpSuccess() {
    console.log('SDP success');
}

function getMedia(isScreenShare, isVideoShare) {

    if (isScreenShare) {
        navigator.mediaDevices.getDisplayMedia().then(stream => {
            stream.oninactive = () => {
                if (selfView.srcObject == null) {
                    return;
                }
                signallingServer.send(JSON.stringify({
                    type: 'stream_closed',
                    kind: "video",
                    id: sessionID
                }));
                var audioTracks = selfView.srcObject.getAudioTracks();
                selfView.srcObject = new MediaStream()
                if (audioTracks.length > 0) {
                    selfView.srcObject.addTrack(audioTracks[0]);
                }
                document.getElementById("self-view").style.display = "none";
                document.getElementById("screenOff").style.display = "none";
                document.getElementById("screen").style.display = "block";
            }

            document.getElementById("flipCamera").style.display = "none";
            document.getElementById("self-view").style.display = "block";
            document.getElementById("screenOff").style.display = "block";
            document.getElementById("screen").style.display = "none";
            document.getElementById("videoOff").style.display = "none";
            document.getElementById("video").style.display = "block";

            if (selfView.srcObject != null) {
                var audioTracks = selfView.srcObject.getAudioTracks()
                var videoTracks = selfView.srcObject.getVideoTracks()
                if (audioTracks.length > 0) {
                    selfView.srcObject.addTrack(stream.getVideoTracks()[0])
                } else {
                    selfView.srcObject = stream
                }
            } else {
                selfView.srcObject = stream
            }

            stream.getTracks().forEach(track => webRtcPeerConnection.addTrack(track, stream))
            createOffer()
        });
    } else if (isVideoShare) {
        navigator.mediaDevices.getUserMedia(mediaConstraints).then(stream => {
            stream.oninactive = () => {
                if (selfView.srcObject == null) {
                    return;
                }

                signallingServer.send(JSON.stringify({
                    type: 'stream_closed',
                    kind: "video",
                    id: sessionID
                }));
                var audioTracks = selfView.srcObject.getAudioTracks();
                document.getElementById("flipCamera").style.display = "none";
                document.getElementById("videoOff").style.display = "none";
                document.getElementById("self-view").style.display = "none";
                document.getElementById("video").style.display = "block";
                selfView.srcObject = new MediaStream()
                if (audioTracks.length > 0) {
                    selfView.srcObject.addTrack(audioTracks[0]);
                }
            }
            document.getElementById("screenOff").style.display = "none";
            document.getElementById("screen").style.display = "block";
            document.getElementById("flipCamera").style.display = "block";
            document.getElementById("self-view").style.display = "block";
            document.getElementById("videoOff").style.display = "block";
            document.getElementById("video").style.display = "none";
            if (selfView.srcObject != null) {
                var audioTracks = selfView.srcObject.getAudioTracks()
                var videoTracks = selfView.srcObject.getVideoTracks()
                if (audioTracks.length > 0) {
                    selfView.srcObject.addTrack(stream.getVideoTracks()[0])
                } else {
                    selfView.srcObject = stream
                }
            } else {
                selfView.srcObject = stream
            }

            selfView.muted = true
            stream.getTracks().forEach(track => webRtcPeerConnection.addTrack(track, stream))
            createOffer()
        });
    } else {
        navigator.mediaDevices.getUserMedia({
            audio: true,
            video: false
        }).then(stream => {
            stream.oninactive = () => {
                document.getElementById("micOff").style.display = "none";
                document.getElementById("mic").style.display = "block";
            }
            document.getElementById("micOff").style.display = "block";
            document.getElementById("mic").style.display = "none";

            if (selfView.srcObject != null) {
                var videoTracks = selfView.srcObject.getVideoTracks()
                if (videoTracks.length > 0)
                    selfView.srcObject.addTrack(stream.getAudioTracks()[0])
                else {
                    document.getElementById("self-view").style.display = "none";
                    selfView.srcObject = stream
                }
            } else {
                document.getElementById("self-view").style.display = "none";
                selfView.srcObject = stream
            }

            selfView.muted = true
            stream.getTracks().forEach(track => webRtcPeerConnection.addTrack(track, stream))
            createOffer()
        });


    }
}

function shareScreen() {
    if (isMobile) {
        alert("Sorry screen share is not supported in mobile!")
        return;
    }
    getMedia(true, false)
}

function shareVideo() {
    if (selfView.srcObject != null && selfView.srcObject != undefined) {
        selfView.srcObject.getVideoTracks().forEach(track => {
            track.stop();
            selfView.srcObject.removeTrack(track)
        });
    }
    getMedia(false, true)
}

function turnOnMic() {
    getMedia(false, false)
}

function closeCall(isManual) {
    doCommonFinalTask()
    onCall = false;
    if (selfView.srcObject != null && selfView.srcObject != undefined) {
        selfView.srcObject.getTracks().forEach(track => {
            track.stop();
        });
        selfView.srcObject = null;
    }
    if (remoteView.srcObject != null && remoteView.srcObject != undefined) {
        remoteView.srcObject = null;
    }
    if (signallingServer != null && signallingServer != undefined && signallingServer.readyState === WebSocket.OPEN)
        signallingServer.close()
    if (webRtcPeerConnection != null && webRtcPeerConnection != undefined && webRtcPeerConnection.connectionState != 'closed')
        webRtcPeerConnection.close();
    if (isManual)
        toastMessage("Session closed")
}

async function createOffer() {
    try {
        var offer = await webRtcPeerConnection.createOffer()
        console.log("offer created")
        webRtcPeerConnection.setLocalDescription(offer);
        signallingServer.send(JSON.stringify({
            type: 'offer',
            offer: offer,
            id: sessionID,
            is_switch_role: !isOfferer
        }));
        isOfferer = true;
    } catch (error) {
        console.log(error)
    }
}

async function createAnswer() {
    try {
        isOfferer = false;
        var answer = await webRtcPeerConnection.createAnswer()
        console.log("answer created")
        webRtcPeerConnection.setLocalDescription(answer);
        signallingServer.send(JSON.stringify({
            type: 'answer',
            answer: answer,
            id: sessionID
        }));
    } catch (error) {
        console.log(error)
    }
}

function doCommonTask() {
    document.getElementById("callid").disabled = true;
    document.getElementById("callid").style.display = "block";
    document.getElementById("join").style.display = "none";
    document.getElementById("call").style.display = "none";
    document.getElementById("screen").style.display = "block";
    document.getElementById("video").style.display = "block";
    document.getElementById("hangUp").style.display = "block";
    document.getElementById("mic").style.display = "block";
}

function doCommonFinalTask() {
    document.getElementById("callid").disabled = false;
    document.getElementById("callid").value = "";
    document.getElementById("join").style.display = "block";
    document.getElementById("call").style.display = "block";
    document.getElementById("screen").style.display = "none";
    document.getElementById("screenOff").style.display = "none";
    document.getElementById("flipCamera").style.display = "none";
    document.getElementById("videoOff").style.display = "none";
    document.getElementById("self-view").style.display = "none";
    document.getElementById("video").style.display = "none";
    document.getElementById("hangUp").style.display = "none";
    document.getElementById("shareid").style.display = "none";
    document.getElementById("mic").style.display = "none";
    document.getElementById("micOff").style.display = "none";
}

function stopVideoShare() {
    signallingServer.send(JSON.stringify({
        type: 'stream_closed',
        kind: "video",
        id: sessionID
    }));

    if (selfView.srcObject != null && selfView.srcObject != undefined) {
        selfView.srcObject.getVideoTracks().forEach(track => {
            track.stop();
            selfView.srcObject.removeTrack(track)
        });
    }
    document.getElementById("self-view").style.display = "none";
    document.getElementById("video").style.display = "block";
    document.getElementById("videoOff").style.display = "none";
    document.getElementById("flipCamera").style.display = "none";
}

function stopShareScreen() {
    signallingServer.send(JSON.stringify({
        type: 'stream_closed',
        kind: "video",
        id: sessionID
    }));
    if (selfView.srcObject != null && selfView.srcObject != undefined) {
        selfView.srcObject.getVideoTracks().forEach(track => {
            track.stop();
            selfView.srcObject.removeTrack(track)
        });
    }
    document.getElementById("screen").style.display = "block";
    document.getElementById("screenOff").style.display = "none";
}

function flipCamera() {
    if (!isMobile) {
        alert("This settings is only applicable for mobile")
        return;
    }
    currentMode = mediaConstraints.video.facingMode
    if (currentMode == "environment")
        mediaConstraints.video.facingMode = "user"
    else
        mediaConstraints.video.facingMode = "environment"
    getMedia(false, true)
}

function shareID() {
    emaild = prompt("Enter recipient email : ")
    if (/\S+@\S+\.\S{2,}/.test(emaild)) {
        signallingServer.send(JSON.stringify({
            type: 'mail',
            mailid: emaild,
            id: sessionID,
            invitorname: name
        }));
        setTimeout(function() {
            toastMessage("Session ID shared to recipient email")
        }, 2000);
        document.getElementById("shareid").style.display = "none"
    } else {
        if (emaild.length < 1)
            toastMessage("Email id cannot be empty")
        else
            toastMessage("Invalid email!")
    }
}

function toastMessage(message) {
    var x = document.getElementById("snackbar");
    x.innerHTML = message;
    x.className = "show";
    setTimeout(function() {
        x.className = x.className.replace("show", "");
    }, 3000);
}

function turnOffMic() {
    document.getElementById("mic").style.display = "block";
    document.getElementById("micOff").style.display = "none";
    if (selfView.srcObject != null && selfView.srcObject != undefined) {
        selfView.srcObject.getAudioTracks().forEach(track => {
            track.stop();
            selfView.srcObject.removeTrack(track)
        });
    }

    signallingServer.send(JSON.stringify({
        type: 'stream_closed',
        kind: "audio",
        id: sessionID
    }));
}