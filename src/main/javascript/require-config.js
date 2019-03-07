var require = {

    baseUrl: "/",
    paths: {
        jquery: '/lib/jquery-1.9.1.min',
        JQUERY: '/javascript/lib/jquery-1.9.1.min',

        //SPLASHPAGE: '/resources/com/cj/nan/etherboard/components/Splashpage'
        SPLASHPAGE: '/components/Splashpage',
        DUMMY: '/javascript/dummy',
        LOGGER: '/javascript/logger'
    },
    shim: {
        jquery: {
            exports: '$'
        },
        JQUERY: {
            exports: '$'
        }
    }

};


