<script lang="ts">
export default {
  onLaunch() {
    // App launch hook for MVP bootstrap.
  },
}
</script>

<style>
@import './styles/tokens.css';

page {
  background: var(--fb-color-bg);
  color: var(--fb-color-text);
  font-size: var(--fb-font-body-sub);
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
