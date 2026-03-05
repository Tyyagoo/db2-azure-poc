param location string
param uaiName string
param acrResourceId string

@description('If empty, AcrPush is not assigned.')
param pushPrincipalObjectId string = ''

module uai 'br/public:avm/res/managed-identity/user-assigned-identity:0.5.0' = {
  name: 'uaiMod'
  params: {
    name: uaiName
    location: location
  }
}

// Built-in role IDs
var acrPullRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d') // AcrPull
var acrPushRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '8311e382-0749-4cb8-b61a-304f252e45ec') // AcrPush

module acrPull 'br/public:avm/ptn/authorization/resource-role-assignment:0.1.2' = {
  name: 'acrPullUai'
  params: {
    principalId: uai.outputs.principalId
    resourceId: acrResourceId
    roleDefinitionId: acrPullRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

module acrPush 'br/public:avm/ptn/authorization/resource-role-assignment:0.1.2' = if (pushPrincipalObjectId != '') {
  name: 'acrPushBuilder'
  params: {
    principalId: pushPrincipalObjectId
    resourceId: acrResourceId
    roleDefinitionId: acrPushRoleDefinitionId
    principalType: 'User'
  }
}

output uaiResourceId string = uai.outputs.resourceId
output uaiClientId string = uai.outputs.clientId
output uaiPrincipalId string = uai.outputs.principalId
