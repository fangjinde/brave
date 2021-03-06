package brave.spring.rabbit;

import brave.Tracing;
import brave.sampler.Sampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import zipkin2.Span;

import static org.assertj.core.api.Assertions.assertThat;

public class TracingMessagePostProcessorTest {

  List<Span> reportedSpans = new ArrayList<>();
  TracingMessagePostProcessor tracingMessagePostProcessor;

  @Before public void setupTracing() {
    reportedSpans.clear();
    Tracing tracing = Tracing.newBuilder()
        .sampler(Sampler.ALWAYS_SAMPLE)
        .spanReporter(reportedSpans::add)
        .build();
    tracingMessagePostProcessor = new TracingMessagePostProcessor(tracing, "my-exchange");
  }

  @Test public void should_add_b3_headers_to_message() {
    Message message = MessageBuilder.withBody(new byte[] {}).build();
    Message postProcessMessage = tracingMessagePostProcessor.postProcessMessage(message);

    List<String> expectedHeaders = Arrays.asList("X-B3-TraceId", "X-B3-SpanId", "X-B3-Sampled");
    Set<String> headerKeys = postProcessMessage.getMessageProperties().getHeaders().keySet();

    assertThat(headerKeys).containsAll(expectedHeaders);
  }

  @Test public void should_report_span() {
    Message message = MessageBuilder.withBody(new byte[] {}).build();
    tracingMessagePostProcessor.postProcessMessage(message);

    assertThat(reportedSpans).hasSize(1);
  }

  @Test public void should_set_remote_service() {
    Message message = MessageBuilder.withBody(new byte[] {}).build();
    tracingMessagePostProcessor.postProcessMessage(message);

    assertThat(reportedSpans.get(0).remoteServiceName())
        .isEqualTo("my-exchange");
  }
}
