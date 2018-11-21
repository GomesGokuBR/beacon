var exec = require('cordova/exec');

exports.initAdaper = function (arg0, success, error) {
    exec(success, error, 'Beacon', 'initAdaper', [arg0]);
};

exports.scan = function (arg0, success, error) {
    exec(success, error, 'Beacon', 'scan', [arg0]);
};

exports.stopScan = function (arg0, success, error) {
    exec(success, error, 'Beacon', 'stopScan', [arg0]);
};

exports.enableBLE = function (arg0, success, error) {
    exec(success, error, 'Beacon', 'enableBLE', [arg0]);
};

exports.purgeBLE = function (arg0, success, error) {
    exec(success, error, 'Beacon', 'purgeBLE', [arg0]);
};



