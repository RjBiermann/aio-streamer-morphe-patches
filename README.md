# 🧩 Morphe patches

Custom patches for the [Morphe](https://morphe.software) patcher.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=RjBiermann/brave-waffle

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0](https://github.com/RjBiermann/brave-waffle/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;4 patches total
<details open>
<summary>📦 AIS&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 6.7.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Remove ads](#remove-ads) | Removes video ad breaks from the standard and popup video players. |  |
| [Remove news promotions](#remove-news-promotions) | Removes the third-party paysite promotion banner from the startup news page. |  |
| [Spoof app signature](#spoof-app-signature) | Reports the original app signature to the API so patched builds are not rejected. |  |
| [Unlock PRO](#unlock-pro) | Unlocks all PRO features permanently. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

AIS Patches are licensed under the [GNU General Public License v3.0](LICENSE)
