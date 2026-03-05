param location string
param acrName string

@allowed([ 'Basic', 'Standard', 'Premium' ])
param sku string = 'Premium'

param adminUserEnabled bool = false

@allowed([ 'Enabled', 'Disabled' ])
param publicNetworkAccess string = 'Disabled'

module acr 'br/public:avm/res/container-registry/registry:0.11.0' = {
  name: 'acrMod'
  params: {
    name: acrName
    location: location
    acrSku: sku
    acrAdminUserEnabled: adminUserEnabled
    publicNetworkAccess: publicNetworkAccess
  }
}

output acrName string = acr.outputs.name
output acrResourceId string = acr.outputs.resourceId
output loginServer string = acr.outputs.loginServer
