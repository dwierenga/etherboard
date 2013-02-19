define(["backbone"], function (Backbone) {
    var Bucket = Backbone.Model.extend({
        defaults: {
            contents: [],
            name: "",
            extraNotes: "",
            height: 150,
            width: 150,
            pos: { left: 0, top: 0 },
            kind: "bucket"
        },
        initialize: function (attributes, options) {
            if (!options || !options.boardName) {
                throw "Must specify boardName";
            }
            this.boardName = options.boardName;
        },
        url: function () {
            return "/board/" + this.boardName + "/objects/" + this.get("id");
        }
    });

    return Bucket;
});