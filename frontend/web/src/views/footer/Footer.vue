<template>
  <footer class="footer">
    <div class="footer-inner">
      <div>
        <strong>知潮 ZENTIDE</strong>
        <p>从兴趣出发，和同好一起看见正在发生的事。</p>
      </div>
      <div class="principles">
        <span>© 2026 ZENTIDE</span>
      </div>
    </div>
  </footer>
</template>
<script setup></script>
<style lang="scss" scoped>
.footer {
  margin-top: 52px;
  padding: 30px 0;
  border-top: 1px solid var(--zt-border);
  color: var(--zt-text);
  background: var(--zt-surface-soft);
}
.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(var(--zt-page-width), calc(100% - 40px));
  margin: auto;
}
.footer strong {
  font-size: 13px;
  letter-spacing: 0.02em;
}
.footer p {
  margin: 5px 0 0;
  color: var(--zt-text-3);
  font-size: 11px;
}
.principles {
  display: flex;
  align-items: center;
  gap: 18px;
  color: var(--zt-text-3);
  font-size: 11px;
}
.principles a {
  color: var(--zt-text-2);
  text-decoration: none;
}
.principles a:hover {
  color: var(--zt-primary);
}
@media (max-width: 680px) {
  .footer {
    margin-top: 30px;
  }
  .footer-inner {
    width: calc(100% - 20px);
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
  .principles {
    flex-wrap: wrap;
    gap: 10px 16px;
  }
}
</style>
