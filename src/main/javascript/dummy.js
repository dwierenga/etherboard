define(["jquery"], function($){

    function getTime(){
        return new Date();
    }

    function popAlert(){

        alert("DBG Dummy alert called")

        console.log("Dummy alert called")
        $('body')
            .append('JQUERY POP ALERT appended to document\n');
        return "Alert returned"
    }

    function getval(){
        debugger;
        return "Got value"
    }

    function appendToPage(msg){
        var txt = "Append to page="+msg
        console.log(txt)
        $('body')
            .append(txt+"\n");
        return txt
    }

	return {
        getTime: getTime,
        popAlert: popAlert,
        getval: getval,
        appendToPage:appendToPage

    };



});
