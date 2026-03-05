param location string
param vmName string
param nsgName string
param nicName string
param subnetId string

param adminUsername string
param adminSshPublicKey string

param githubOwner string
param githubRepo string

@secure()
param githubPat string

param runnerLabels string = 'linux,x64,private-vnet'

// NSG (PoC: allow SSH from VNet only)
resource nsg 'Microsoft.Network/networkSecurityGroups@2025-05-01' = {
  name: nsgName
  location: location
  properties: {
    securityRules: [
      {
        name: 'AllowSshFromVnet'
        properties: {
          priority: 100
          direction: 'Inbound'
          access: 'Allow'
          protocol: 'Tcp'
          sourcePortRange: '*'
          destinationPortRange: '22'
          sourceAddressPrefix: 'VirtualNetwork'
          destinationAddressPrefix: '*'
        }
      }
    ]
  }
}

resource nic 'Microsoft.Network/networkInterfaces@2025-05-01' = {
  name: nicName
  location: location
  properties: {
    networkSecurityGroup: {
      id: nsg.id
    }
    ipConfigurations: [
      {
        name: 'ipconfig1'
        properties: {
          privateIPAllocationMethod: 'Dynamic'
          subnet: {
            id: subnetId
          }
        }
      }
    ]
  }
}

var cloudInit = base64($$'''#cloud-config
package_update: true
packages:
  - ca-certificates
  - curl
  - jq
  - git

write_files:
  - path: /opt/github-runner/register.sh
    permissions: '0755'
    content: |
      #!/usr/bin/env bash
      set -euo pipefail

      mkdir -p /opt/actions-runner
      cd /opt/actions-runner

      # Install Docker
      curl -fsSL https://get.docker.com | sh
      usermod -aG docker $${adminUsername}

      # Download the latest runner (robust: pull asset URL from GitHub API)
      LATEST_JSON="$(curl -fsSL https://api.github.com/repos/actions/runner/releases/latest)"

      ASSET_URL="$(echo "${LATEST_JSON}" | jq -r '.assets[] | select(.name | test("^actions-runner-linux-x64-.*\\.tar\\.gz$")) | .browser_download_url' | head -n 1)"

      if [ -z "${ASSET_URL}" ] || [ "${ASSET_URL}" = "null" ]; then
        echo "ERROR: Could not find runner asset URL in GitHub release JSON"
        exit 1
      fi

      RUNNER_TGZ="$(basename "${ASSET_URL}")"
      curl -fsSL -o "${RUNNER_TGZ}" "${ASSET_URL}"
      tar xzf "${RUNNER_TGZ}"

      # Mint a short-lived registration token using PAT (repo-level runner)
      REG_TOKEN="$(curl -fsSL -X POST \
        -H "Authorization: Bearer $${githubPat}" \
        -H "Accept: application/vnd.github+json" \
        "https://api.github.com/repos/$${githubOwner}/$${githubRepo}/actions/runners/registration-token" \
        | jq -r .token)"

      ./config.sh --unattended \
        --url "https://github.com/$${githubOwner}/$${githubRepo}" \
        --token "$REG_TOKEN" \
        --name "$${vmName}" \
        --labels "$${runnerLabels}" \
        --work "_work" \
        --replace

      ./svc.sh install $${adminUsername}
      ./svc.sh start

runcmd:
  - /opt/github-runner/register.sh
''')

resource buildVm 'Microsoft.Compute/virtualMachines@2025-04-01' = {
  name: vmName
  location: location
  properties: {
    hardwareProfile: {
      vmSize: 'Standard_D2s_v3'
    }
    osProfile: {
      computerName: vmName
      adminUsername: adminUsername
      linuxConfiguration: {
        disablePasswordAuthentication: true
        ssh: {
          publicKeys: [
            {
              path: '/home/${adminUsername}/.ssh/authorized_keys'
              keyData: adminSshPublicKey
            }
          ]
        }
      }
      customData: cloudInit
    }
    storageProfile: {
      imageReference: {
        publisher: 'Canonical'
        offer: '0001-com-ubuntu-server-jammy'
        sku: '22_04-lts-gen2'
        version: 'latest'
      }
      osDisk: {
        createOption: 'FromImage'
        managedDisk: {
          storageAccountType: 'Premium_LRS'
        }
      }
    }
    networkProfile: {
      networkInterfaces: [
        { id: nic.id }
      ]
    }
  }
}

output buildVmName string = buildVm.name
