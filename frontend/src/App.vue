<script lang="ts">
const hideNativeTabBar = () => {
  uni.hideTabBar({ animation: false })
}

export default {
  onLaunch() {
    hideNativeTabBar()
  },
  onShow() {
    hideNativeTabBar()
  },
}
</script>

<style>
@import './styles/tokens.css';

page {
  background: #f8fafb;
  color: #1a1a1a;
  font-size: 28rpx;
  font-family: 'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
}

view,
text,
input,
textarea,
button {
  box-sizing: border-box;
}
</style>

