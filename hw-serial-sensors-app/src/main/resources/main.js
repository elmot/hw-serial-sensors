var vaadinWindowContent;

/*
 *  Service stuff
 *
 */
function doLog(logMsg, italic) {
    console.log(logMsg);
    var str = italic ? ("<i>" + logMsg + "</i>") : logMsg;
    var buffer = document.getElementById("buffer");
    buffer.innerHTML += str + "<br>";
    buffer.parentElement.scrollTop = 100000
}

/*
 *  Messaging
 *
 */

function vaadinCallBack(type, success, message, data) {
    if (success == null) success = true;
    if (!success) message = message || chrome.runtime.lastError.message;
    vaadinWindowContent.postMessage({
        "type": type,
        "success": success,
        "message": message,
        "data": data
    }, "*");
}

/* Message receive */
window.addEventListener("message",
    function (msg) {
        if (msg.data) {
            switch (msg.data.type) {
                case "send":
                    writeSerial(msg.data.text);
                    break;
                case "connect":
                    connect();
                    break;
                case "disconnect":
                    disconnect(false);
                    break;
            }
        }
    });

document.getElementById("vaadinApp").addEventListener("contentload",
    function () {
        // save a webview content object for a future use
        vaadinWindowContent = document.getElementById("vaadinApp").contentWindow;
        // handshake
        vaadinCallBack("init");
    });
