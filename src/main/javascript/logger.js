define([], function(){

    function writeToLog(msg){
        var txt = "Append to page="+msg
        console.log(txt)
        return txt
    }

    return {
        writeToLog: writeToLog
    };
});