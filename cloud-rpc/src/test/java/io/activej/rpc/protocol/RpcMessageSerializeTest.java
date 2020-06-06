package io.activej.rpc.protocol;

import io.activej.serializer.BinarySerializer;
import io.activej.serializer.SerializerBuilder;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import io.activej.test.rules.ByteBufRule;
import org.junit.ClassRule;
import org.junit.Test;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class RpcMessageSerializeTest {

	public static class TestRpcMessageData {
		private final String s;

		public TestRpcMessageData(@Deserialize("s") String s) {
			this.s = s;
		}

		@Serialize(order = 0)
		public String getS() {
			return s;
		}

	}

	public static class TestRpcMessageData2 {
		private final int i;

		public TestRpcMessageData2(@Deserialize("i") int i) {
			this.i = i;
		}

		@Serialize(order = 0)
		public int getI() {
			return i;
		}

	}

	@ClassRule
	public static final ByteBufRule byteBufRule = new ByteBufRule();

	@Test
	public void testRpcMessage() {
		TestRpcMessageData messageData1 = new TestRpcMessageData("TestMessageData");
		RpcMessage message1 = RpcMessage.of(1, messageData1);
		BinarySerializer<RpcMessage> serializer = SerializerBuilder.create(getSystemClassLoader())
				.withSubclasses(RpcMessage.MESSAGE_TYPES, TestRpcMessageData.class, TestRpcMessageData2.class)
				.build(RpcMessage.class);

		byte[] buf = new byte[1000];
		serializer.encode(buf, 0, message1);
		RpcMessage message2 = serializer.decode(buf, 0);
		assertEquals(message1.getCookie(), message2.getCookie());
		assertTrue(message2.getData() instanceof TestRpcMessageData);
		TestRpcMessageData messageData2 = (TestRpcMessageData) message2.getData();
		assertEquals(messageData1.getS(), messageData2.getS());
	}
}