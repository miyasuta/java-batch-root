const cds = require('@sap/cds')

module.exports = class CatalogService extends cds.ApplicationService {
    init () {
        this.before('READ', 'SalesOrders', req => {
            console.log('read handler');
        })
        return super.init();
    }
}