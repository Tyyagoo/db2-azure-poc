param location string = resourceGroup().location

@description('Short project identifier used to name all resources (e.g., "quarkus-db2").')
param projectName string = 'db2poc'

@allowed([ 'dev', 'tqs', 'hmp', 'prd' ])
@description('Deployment environment.')
param env string = 'dev'

@description('Optional instance number (e.g. 01) if you deploy multiple stacks per env/region.')
param instance string = '01'

@description('Optional: Object ID (GUID) of the principal that will PUSH images (you or CI). If empty, no AcrPush assignment is created.')
param pushPrincipalObjectId string = '21d1ea64-2beb-422e-aa38-67be57495649'

@description('Linux admin username')
param adminUsername string = 'azureuser'

@description('SSH public key for admin user')
param adminSshPublicKey string

@description('GitHub owner/org (e.g. mybank)')
param githubOwner string = 'Tyyagoo'

@description('GitHub repo name (e.g. quarkus-db2-poc)')
param githubRepo string = 'db2-azure-poc'

@secure()
@description('GitHub PAT used ONLY to mint a short-lived runner registration token at boot.')
param githubPat string

@description('Runner label(s) comma-separated (e.g. "linux,x64,private-vnet").')
param runnerLabels string = 'linux,x64,private-vnet'

// Networking
param vnetAddressSpace string = '10.40.0.0/16'
param peSubnetPrefix string = '10.40.1.0/24'
param acaSubnetPrefix string = '10.40.2.0/23'
param buildSubnetPrefix string = '10.40.4.0/24'

// ---------- Naming ----------
var projLc = toLower(projectName)
var projBase0 = replace(replace(replace(replace(projLc, '-', ''), '_', ''), '.', ''), ' ', '')
var projBase = length(projBase0) < 3 ? '${projBase0}proj' : projBase0
var regionPart = toLower(location)
var uniq = toLower(take(uniqueString(subscription().id, resourceGroup().id, projBase, env, regionPart, instance), 6))

var vnetName = take('${projBase}-${env}-${regionPart}-vnet-${instance}-${uniq}', 64)

var peName = take('${projBase}-${env}-${regionPart}-pe-acr-${instance}-${uniq}', 80)

// ACR: 5-50 chars, lowercase alphanumeric only
var acrName = toLower(take('${projBase}${env}${regionPart}acr${uniq}', 50))

// UAI name: safe truncation
var uaiName = take('${projBase}-${env}-${regionPart}-uai-${instance}-${uniq}', 60)

// Runner VM names
var vmName = take('${projBase}-${env}-${regionPart}-vm-build-${instance}-${uniq}', 63)
var nsgName = take('${projBase}-${env}-${regionPart}-nsg-build-${instance}-${uniq}', 80)
var nicName = take('${projBase}-${env}-${regionPart}-nic-build-${instance}-${uniq}', 80)

// ---------- Modules ----------

// 1) Network (VNet + subnets)
module net 'modules/network.bicep' = {
  name: 'network'
  params: {
    location: location
    vnetName: vnetName
    vnetAddressSpace: vnetAddressSpace
    peSubnetName: 'snet-pe'
    peSubnetPrefix: peSubnetPrefix
    acaSubnetName: 'snet-aca'
    acaSubnetPrefix: acaSubnetPrefix
    buildSubnetName: 'snet-build'
    buildSubnetPrefix: buildSubnetPrefix
  }
}

// 2) ACR (AVM)
module acr 'modules/acr/main.bicep' = {
  name: 'acr'
  params: {
    location: location
    acrName: acrName
    sku: 'Premium'
    publicNetworkAccess: 'Disabled'
    adminUserEnabled: false
  }
}

// 3) ACR Private Link (Private DNS + VNet link + PE + Zone Group)
module acrPl 'modules/acr/private-link.bicep' = {
  name: 'acrPrivateLink'
  params: {
    location: location
    peName: peName
    vnetId: net.outputs.vnetId
    peSubnetId: net.outputs.peSubnetId
    acrId: acr.outputs.acrResourceId
  }
}

// 4) Identity + RBAC (UAI + AcrPull + AcrPush)
module id 'modules/rbac.bicep' = {
  name: 'identity'
  params: {
    location: location
    uaiName: uaiName
    acrResourceId: acr.outputs.acrResourceId
    pushPrincipalObjectId: pushPrincipalObjectId
  }
}

// 5) Build runner VM (GitHub self-hosted runner) in build subnet
module runner 'modules/runner-vm-github.bicep' = {
  name: 'runner'
  params: {
    location: location
    vmName: vmName
    nsgName: nsgName
    nicName: nicName
    subnetId: net.outputs.buildSubnetId

    adminUsername: adminUsername
    adminSshPublicKey: adminSshPublicKey

    githubOwner: githubOwner
    githubRepo: githubRepo
    githubPat: githubPat
    runnerLabels: runnerLabels
  }
}

// ---------- Outputs ----------
output vnetId string = net.outputs.vnetId
output peSubnetId string = net.outputs.peSubnetId
output buildSubnetId string = net.outputs.buildSubnetId

output acrPrivateEndpointId string = acrPl.outputs.privateEndpointId
output acrPrivateDnsZoneId string = acrPl.outputs.privateDnsZoneId

output acrLoginServer string = acr.outputs.loginServer
output acrResourceId string = acr.outputs.acrResourceId

output uaiResourceId string = id.outputs.uaiResourceId
output uaiClientId string = id.outputs.uaiClientId
output uaiPrincipalId string = id.outputs.uaiPrincipalId

output buildVmName string = runner.outputs.buildVmName
