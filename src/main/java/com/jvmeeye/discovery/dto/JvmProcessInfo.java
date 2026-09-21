package com.jvmeeye.discovery.dto;

/**
 * 本机一个可发现 JVM 的描述。
 *
 * @param pid          进程号
 * @param displayName  VirtualMachineDescriptor 的显示名(通常是 main class)
 * @param mainClass    主类(sun.java.command 的第一段)
 * @param mainArgs     主类参数
 * @param jvmVersion   java.version
 * @param jvmName      java.vm.name
 * @param javaHome     java.home
 * @param user         进程属主(通过 /proc 或 ProcessHandle 尽力获取)
 * @param attachable   是否可以 attach(同用户、同架构、权限允许)
 * @param self         是否为 JvmEye 自身进程
 * @param reason       attachable=false 时的原因
 */
public record JvmProcessInfo(
        long pid,
        String displayName,
        String mainClass,
        String mainArgs,
        String jvmVersion,
        String jvmName,
        String javaHome,
        String user,
        boolean attachable,
        boolean self,
        String reason) {
}
