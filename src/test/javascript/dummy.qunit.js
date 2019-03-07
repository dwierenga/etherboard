define(["jquery", "dummy"], function($, dummy){

	test("finds the body element", function(){
		// when
		var body = $("body");
		
		// then
		equal(body.length, 1);
	});

    test("dummy tells the time", function(){
        // when
        var time = dummy.getTime();

        // then
        ok(time);
    });

    test("dummy displays alert", function(){
        // when
        var result = dummy.getval();

        // then
        equal(result, "Got value");
    });

    test("dummy displays alert", function(){
        // when
        var result = dummy.popAlert();

        // then
        equal(result, "Alert returned");
    });

    test("dummy appends to page", function(){
        // when
        msg = ""

        var msg = "test1",
            expected = "Append to page="+msg
            result = dummy.appendToPage(msg);

        // then
        equal(result, expected);
    });
	
});
