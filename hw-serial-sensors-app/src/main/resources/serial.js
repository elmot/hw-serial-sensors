var expectedConnectionId;

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

function convertArrayBufferToString(buf) {
    var bufView = new Uint8Array(buf);
    return String.fromCharCode.apply(null, bufView);
}

/*
 *  COM port stuff
 *
 */

function disconnect(reconnectAfter) {
    if (expectedConnectionId == null) return;
        doLog("Disconnecting", true);
    chrome.serial.disconnect(expectedConnectionId,
        function (q) {
            if (!q) doLog("Disconnect Failed");

            if (reconnectAfter) connect(); else vaadinCallBack("disconnect", q);
        }
    );
    expectedConnectionId = null;
}

function connect() {
    if (expectedConnectionId != null) {
        disconnect(true);
    }
    doLog("Looking for serial ports", true);
    chrome.serial.getDevices(function (ports) {
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
            vaadinCallBack("connect", false, "Port not found");
        }
    });
}

function connectedHandler(connectInfo) {
    if (connectInfo == null) {
        doLog("Connection failed", true);
        vaadinCallBack("connect", false);
    } else {
        doLog("Connected.", true);
        expectedConnectionId = connectInfo.connectionId;
        vaadinCallBack("connect");
    }
}

function writeSerial(str) {
    doLog("S: " + str);
    if (expectedConnectionId == null) {
        doLog("Not connected", true);
        return false
    }
    chrome.serial.send(expectedConnectionId, convertStringToArrayBuffer(str),
        function (sendInfo) {
            if (sendInfo.error) {
                doLog(sendInfo.error, true);
                vaadinCallBack("writeSerial", false, sendInfo.error);
            } else {
                chrome.serial.flush(expectedConnectionId, function (q) {
                    vaadinCallBack("flush", q);
                });
            }
        });
}

chrome.serial.onReceiveError.addListener(function (info) {
    vaadinCallBack("readSerial", false, info.error);
});

chrome.serial.onReceive.addListener(function (info) {
    if (info.connectionId == expectedConnectionId && info.data) {
        stringReceived += convertArrayBufferToString(info.data);
        var lines = stringReceived.split("\n");
        for (var i = 0; i < lines.length - 1; i++) {
            var line = lines[i];
            if (line != "") {
                //doLog("R: " + line);
                vaadinCallBack("readSerial", true, null, line);
            }
        }
        stringReceived = lines[lines.length - 1]
    }
});
