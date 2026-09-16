<script setup lang="ts">
import NavigationBar from "../components/NavigationBar.vue";
</script>

<template>
  <div class="background-container">
    <NavigationBar/>
    <router-view />
  </div>
</template>

<style scoped>
.background-container {
  display: flex;
  flex-direction: column;
}
</style>
