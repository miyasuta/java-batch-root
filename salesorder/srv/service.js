const cds = require('@sap/cds')

module.exports = class CatalogService extends cds.ApplicationService {
    init () {
        this.before('READ', 'SalesOrders', req => {
            console.log('read handler');
        })

        this.before('CREATE', 'SalesOrders', req => {
            console.log('request data: ', req.data);
        })
        return super.init();
    }
}