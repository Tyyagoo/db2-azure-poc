param location string
param peName string
param vnetId string
param peSubnetId string
param acrId string

resource acrPrivateDns 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: 'privatelink.azurecr.io'
  location: 'global'
}

var vnetLinkName = guid(acrPrivateDns.id, vnetId)

resource acrDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  name: vnetLinkName
  parent: acrPrivateDns
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnetId
    }
    registrationEnabled: false
  }
}

resource acrPe 'Microsoft.Network/privateEndpoints@2025-05-01' = {
  name: peName
  location: location
  properties: {
    subnet: {
      id: peSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'acr-connection'
        properties: {
          privateLinkServiceId: acrId
          groupIds: [
            'registry'
          ]
        }
      }
    ]
  }
}

resource acrPeDnsGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2025-05-01' = {
  name: 'acr-dns'
  parent: acrPe
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'acrZone'
        properties: {
          privateDnsZoneId: acrPrivateDns.id
        }
      }
    ]
  }
}

output privateEndpointId string = acrPe.id
output privateDnsZoneId string = acrPrivateDns.id
