param location string
param vnetName string

param vnetAddressSpace string

param peSubnetName string
param peSubnetPrefix string

param acaSubnetName string
param acaSubnetPrefix string

param buildSubnetName string
param buildSubnetPrefix string

resource vnet 'Microsoft.Network/virtualNetworks@2025-05-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        vnetAddressSpace
      ]
    }
  }
}

resource peSubnet 'Microsoft.Network/virtualNetworks/subnets@2025-05-01' = {
  parent: vnet
  name: peSubnetName
  properties: {
    addressPrefixes: [
      peSubnetPrefix
    ]
    // Required for Private Endpoints
    privateEndpointNetworkPolicies: 'Disabled'
  }
}

resource acaSubnet 'Microsoft.Network/virtualNetworks/subnets@2025-05-01' = {
  parent: vnet
  name: acaSubnetName
  properties: {
    addressPrefixes: [
      acaSubnetPrefix
    ]
  }
}

resource buildSubnet 'Microsoft.Network/virtualNetworks/subnets@2025-05-01' = {
  parent: vnet
  name: buildSubnetName
  properties: {
    addressPrefixes: [
      buildSubnetPrefix
    ]
  }
}

output vnetId string = vnet.id
output peSubnetId string = peSubnet.id
output acaSubnetId string = acaSubnet.id
output buildSubnetId string = buildSubnet.id
