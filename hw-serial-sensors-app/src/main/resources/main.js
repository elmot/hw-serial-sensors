const serial = chrome.serial;
var expectedConnectionId;
var vaadinWindowContent;

var stringReceived = '';

/*
 *  Service stuff
 *
 */

function convertStringToArrayBuffer(str) {
    var buf = new ArrayBuffer(str.length);
    var bufView = new Uint8Array(buf);
    for (var i = 0; i < str.length; i++) {
        bufView[i] = str.charCodeAt(i);
    }
    return buf;
}
function doLog(logMsg, italic) {
    console.log(logMsg);
    var str = italic ? ("<i>" + logMsg + "</i>") : logMsg;
    var buffer = document.getElementById("buffer");
    buffer.innerHTML += str + "<br>";
    buffer.parentElement.scrollTop = 100000
}

function convertArrayBufferToString(buf) {
    var bufView = new Uint8Array(buf);
    var encodedString = String.fromCharCode.apply(null, bufView);
    return decodeURIComponent(escape(encodedString));
}

/* Call Vaadin callback*/
function serialCallBack(type, success, message, data) {
    if (success == null) success = true;
    if (!success) {
        message = message || chrome.runtime.lastError.message;
    }
    vaadinWindowContent.postMessage({
        "type": "callback", "payload": {
            "type": type,
            "success": success,
            "message": message,
            "data": data
        }
    }, "*");


}
/*
 *  COM port stuff
 *
 */

function disconnect(reconnectAfter) {
    serial.disconnect(expectedConnectionId,
        function (q) {
            if (!q) {
                doLog("Disconnect Failed");
            }
            if (reconnectAfter) {
                connect();
            } else {
                serialCallBack("disconnect", q);
            }
        }
    );
    expectedConnectionId = null;
}

function connect() {
    if (expectedConnectionId != null) {
        disconnect(true);
        return;
    }
    doLog("Looking for serial ports", true);
    serial.getDevices(function (ports) {
        var foundSerial = null;
        for (var i = 0; i < ports.length; i++) {
            var name = ports[i].displayName;
            var portDesc = ports[i].path + ": " + name;
            if (name == "STMicroelectronics STLink Virtual COM Port") {
                foundSerial = ports[i];
                portDesc += "[Actual]"
            }
            doLog("Found: " + portDesc);
        }
        if (foundSerial != null) {
            doLog("Connecting " + foundSerial.path, true);
            chrome.serial.connect(foundSerial.path, {"bitrate": 115200}, connectedHandler)
        } else {
            doLog("Port is not found ", true);
            serialCallBack("connect", false, "Port not found");
        }
    });
}


function writeSerial(str) {
    doLog("S: " + str);
    if (expectedConnectionId == null) {
        doLog("Not connected", true);
        return false
    }
    serial.send(expectedConnectionId, convertStringToArrayBuffer(str),
        function (sendInfo) {
            if (sendInfo.error) {
                doLog(sendInfo.error, true);
                serialCallBack("writeSerial", false, sendInfo.error);
            } else {
                serial.flush(expectedConnectionId, function (q) {
                    serialCallBack("flush", q);
                });
            }
        });
}


function connectedHandler(connectInfo) {
    if (connectInfo == null) {
        doLog("Connection failed", true);
        serialCallBack("connect", false);
    } else {
        doLog("Connected.", true);
        expectedConnectionId = connectInfo.connectionId;
        serialCallBack("connect");
    }
}

serial.onReceiveError.addListener(function (info) {
    serialCallBack("readSerial", false, info.error);
});

serial.onReceive.addListener(function (info) {
    if (info.connectionId == expectedConnectionId && info.data) {
        stringReceived += convertArrayBufferToString(info.data);
        var lines = stringReceived.split("\n");
        for (var i = 0; i < lines.length - 1; i++) {
            var line = lines[i];
            if (line != "") {
                //doLog("R: " + line);
                serialCallBack("readSerial", true, null, line);
            }
        }
        stringReceived = lines[lines.length - 1]
    }
});

/*
 *  Messaging
 *
 */


window.addEventListener("message",
    function (e) {
        if (e.data) {
            switch (e.data.type) {
                case "send":
                    writeSerial(e.data.text);
                    break;
                case "connect":
                    if (expectedConnectionId == null) {
                        connect();
                    }
                    break;
                case "disconnect":
                    if (expectedConnectionId != null) {
                        doLog("Disconnecting", true);
                        disconnect(false);
                    }
                    break;
            }
        }
    });

document.getElementById("vaadinApp").addEventListener("contentload",
    function () {
        vaadinWindowContent = document.getElementById("vaadinApp").contentWindow;
        vaadinWindowContent.postMessage({type: "init"}, "*");
    });
