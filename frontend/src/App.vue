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
  background: #faf7f2;
  color: #302e29;
  font-size: 28rpx;
  font-family: 'Songti SC', 'STSong', 'SimSun', Georgia, serif;
}

view,
text,
input,
textarea,
button {
  box-sizing: border-box;
}
</style>

