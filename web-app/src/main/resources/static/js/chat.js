var socket;
var joined = false;
var lockUpload = false
var joining = false
var rejoining = false;
var fileOrdinal = 0;
var cancelCurrentFile = false;
var fileOffset = 0;

function join() {
    if (document.getElementById("name").value.length < 1) {
        alert("Invalid name!");
        return;
    }
    if (joining)
        return
    document.getElementById("join").innerHTML = 'Joining ..'
    joining = true
    var url = "ws://";
    if (window.location.origin.includes('https'))
        url = "wss://";

    url = url + window.location.host + "/api/v1/chat?name=" + document.getElementById("name").value + "&rejoin=" + rejoining;
    socket = new WebSocket(url);
    socket.addEventListener('open', function(event) {
        joined = true;
        joining = false
        document.getElementById("name").style.fontWeight = "bold";
        document.getElementById("chatbox").innerHTML = ''
        document.getElementById("name").disabled = true;
        document.getElementById("file").disabled = false;
        document.getElementById("join").style.display = "none"
        document.getElementById("leave").style.display = "block"
    });
    socket.addEventListener("error", (event) => {
        document.getElementById("join").innerHTML = 'Join Chat'
        joining = false
        joined = false;
        lockUpload = false;
        document.getElementById("file").disabled = true;
        document.getElementById("chatbox").innerHTML = document.getElementById("chatbox").innerHTML + "<b style='color:red;margin: 140px''>Disconnected!</b><br><br>";
        document.getElementById("chatbox").scrollTo(0, document.getElementById("chatbox").scrollHeight);
        console.log(event);
    });
    socket.addEventListener('close', function(event) {
        console.log(event)
        console.log("Session closed at " + new Date())
        document.getElementById("file").disabled = true;
//        if (event.code == 1006) {
//            //rejoining = true;
//            //setTimeout(join, 1000)
//            return;
//        }
        document.getElementById("join").innerHTML = 'Join Chat'
        joining = false
        lockUpload = false;
        if (event.reason == "duplicate_name") {
            alert("Session already exists with this name. Please try again later.")
            document.getElementById("name").disabled = false;
            document.getElementById("join").style.display = "block"

        }
        document.getElementById("join").style.display = "block"
        document.getElementById("leave").style.display = "none"
        if (joined) {
            document.getElementById("chatbox").innerHTML = document.getElementById("chatbox").innerHTML + "<b style='color:red;margin: 140px''>Disconnected!</b><br><br>";
            document.getElementById("chatbox").scrollTo(0, document.getElementById("chatbox").scrollHeight);
        }
        joined = false;
        document.getElementById("filename").innerHTML = ""
        document.getElementById("file").style.display = "block"
        document.getElementById("cancel").style.display = "none"
        document.getElementById("file").value = "";


    });
    socket.addEventListener('message', function(event) {
        incoming(event.data);
    });
}

function incoming(msg) {

//    if (rejoining) {
//        document.getElementById("chatbox").innerHTML = document.getElementById("chatbox").innerHTML + "<b style='color:green;margin: 140px''>Rejoined!</b><br><br>";
//        rejoining = false;
//        document.getElementById("chatbox").scrollTo(0, document.getElementById("chatbox").scrollHeight);
//        return;
//    }

    if (msg.startsWith("fileuploaddone123") || msg.startsWith("currentFileCancelled")) {
        fileOffset = 0;

        if (msg.startsWith("currentFileCancelled"))
            cancelCurrentFile = false;

        if (fileOrdinal < document.querySelector("#file").files.length - 1) {
            console.log("Upload completed for " + document.querySelector("#file").files[fileOrdinal].name)
            fileOrdinal += 1
            handleFileUpload(fileOrdinal)
            return
        }
        document.getElementById("file").value = "";
        document.getElementById("filename").innerHTML = ""
        document.getElementById("file").style.display = "block"
        lockUpload = false
        fileOrdinal = 0;
        document.getElementById("cancel").style.display = "none"
        return
    }
    if (msg.includes("Uploading") && msg.includes("% completed")) {
        document.getElementById("filename").innerHTML = "(" + (fileOrdinal) + "/" + document.querySelector("#file").files.length + ") " + msg
        sendFileChunks(document.querySelector("#file").files[fileOrdinal]);
        return;
    }
    document.getElementById("incomingSound").play()
    document.getElementById("chatbox").innerHTML = document.getElementById("chatbox").innerHTML + msg;
    document.getElementById("chatbox").scrollTo(0, document.getElementById("chatbox").scrollHeight);
}

async function sendMsg() {
    if (joined) {
        if (document.querySelector("#file").files.length >= 1 && lockUpload == false) {
            lockUpload = true
            handleFileUpload(0);
        }
        if (document.getElementById("chbox").value.length >= 1) {
            if (lockUpload) {
                alert("Please send the message once the upload is completed");
                return
            }
            socket.send(document.getElementById("chbox").value);
            document.getElementById("chbox").value = "";
        }
    } else
        alert("Click on Join Chat to send message")
}

const chunkSize = 1024 * 50;

function sendFileChunks(file) {
    if (cancelCurrentFile) {
        return;
    }

    const reader = new FileReader();

    const blob = file.slice(fileOffset, fileOffset + chunkSize);

    reader.onload = function(event) {
        const chunkData = event.target.result;

        if (cancelCurrentFile) {
            return;
        }

        if (chunkData.byteLength > 0 && !cancelCurrentFile) {
            //console.log("sending ..")
            socket.send(chunkData);
            fileOffset += chunkSize;
        } else if (!cancelCurrentFile) {
            socket.send("endoffile123");
            return;
        }
    };

    reader.readAsArrayBuffer(blob);
}



function leave() {
    joined = false;
    document.getElementById("incomingSound").play()
    socket.close();
    document.getElementById("chatbox").innerHTML = document.getElementById("chatbox").innerHTML + "<b style='color:red;margin: 140px''>You Left!</b></br></br>";
    document.getElementById("chatbox").scrollTo(0, document.getElementById("chatbox").scrollHeight);
    document.getElementById("join").style.display = "block"
    document.getElementById("leave").style.display = "none"
}

function loadFile() {
    if (document.querySelector("#file").files[0] != null && lockUpload == false && joined) {
        document.getElementById("filename").innerHTML = "Uploading ...";
        sendMsg()
    }
}

function handleFileUpload(fileOrdinal) {
    document.getElementById("cancel").style.display = "block"
    document.getElementById("cancel").innerHTML = "[ Cancel " + document.querySelector("#file").files[fileOrdinal].name + " ]"
    socket.send("filename=" + document.querySelector("#file").files[fileOrdinal].name + "&size=" + document.querySelector("#file").files[fileOrdinal].size)
    document.getElementById("file").style.display = "none"
    sendFileChunks(document.querySelector("#file").files[fileOrdinal]);
}

setInterval(loadFile, 1000);

function cancelFileUpload() {
    if (cancelCurrentFile) {
        return;
    }
    document.getElementById("filename").innerHTML = "Cancelling ...";
    cancelCurrentFile = true;
    socket.send("cancelCurrentFile");
}

function joinNow(event) {
    if (event != undefined && event.key == 'Enter')
        join()
}