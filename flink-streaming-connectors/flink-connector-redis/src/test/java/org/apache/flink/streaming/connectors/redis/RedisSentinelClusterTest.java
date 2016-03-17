package org.apache.flink.streaming.connectors.redis;

import org.apache.flink.streaming.connectors.redis.common.config.JedisSentinelConfig;
import org.apache.flink.streaming.connectors.redis.common.container.RedisCommandsContainer;
import org.apache.flink.streaming.connectors.redis.common.container.RedisCommandsContainerBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.embedded.RedisCluster;
import redis.embedded.util.JedisUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.apache.flink.util.NetUtils.getAvailablePort;

public class RedisSentinelClusterTest {

	private static RedisCluster cluster;
	private static final String REDIS_MASTER = "master";
	private static final List<Integer> sentinels = Arrays.asList(getAvailablePort(), getAvailablePort());
	private static final List<Integer> group1 = Arrays.asList(getAvailablePort(), getAvailablePort());

	private JedisSentinelPool jedisSentinelPool;
	private JedisSentinelConfig jedisSentinelConfig;

	@BeforeClass
	public static void setUpCluster(){
		cluster = RedisCluster.builder().sentinelPorts(sentinels).quorumSize(1)
			.serverPorts(group1).replicationGroup(REDIS_MASTER, 1)
			.build();
		cluster.start();
	}

	@Before
	public void setUp() {
		Set<String> hosts = JedisUtil.sentinelHosts(cluster);
		jedisSentinelConfig = new JedisSentinelConfig.Builder().setMasterName(REDIS_MASTER)
			.setSentinels(hosts).build();
		jedisSentinelPool = new JedisSentinelPool(jedisSentinelConfig.getMasterName(),
			jedisSentinelConfig.getSentinels());
	}

	@Test
	public void testRedisSentinelOperation() {
		RedisCommandsContainer redisContainer = RedisCommandsContainerBuilder.build(jedisSentinelConfig);
		Jedis jedis = null;
		try{
			jedis = jedisSentinelPool.getResource();
			redisContainer.set("testKey", "testValue");
			assertEquals("testValue", jedis.get("testKey"));
		}catch (Exception ignore){

		}finally {
			if (jedis != null){
				jedis.close();
			}
		}
	}

	@After
	public void tearDown() throws IOException {
		if (jedisSentinelPool != null)
			jedisSentinelPool.close();
	}

	@AfterClass
	public static void tearDownCluster() throws IOException {
		if (!cluster.isActive())
			cluster.stop();
	}
}