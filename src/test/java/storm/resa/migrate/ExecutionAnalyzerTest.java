package storm.resa.migrate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import storm.resa.util.RedisQueueIterable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExecutionAnalyzerTest {

    private Iterable<String> source;

    @Before
    public void init() {
        source = new RedisQueueIterable("192.168.0.30", 6379, "wc-metrics-128", 1000000);
    }

    @After
    public void cleanup() {
        if (source instanceof Closeable) {
            try {
                ((Closeable) source).close();
            } catch (IOException e) {
            }
        }
    }

    @Test
    public void testCalcStat() throws Exception {
        int exeCnt = 16;
        ExecutionAnalyzer analyzer = new ExecutionAnalyzer(source);
        analyzer.calcStat();

        analyzer.getStat().forEach((k, v) -> {
            System.out.println(k + "->" + v);
        });

        Map<String, ExecutionAnalyzer.ExecutionStat> data = analyzer.getStat().subMap("counter", "counter;");
        data.forEach((k, v) -> System.out.println(v.getCount()));
        System.out.println("avg:" + data.entrySet().stream().mapToLong(e -> e.getValue().getCount()).average());
        int[] exe2Tasks = computeTasks(data.size(), exeCnt);
        Iterator<Map.Entry<String, ExecutionAnalyzer.ExecutionStat>> dataIterator = data.entrySet().iterator();
        List<ExecutionAnalyzer.ExecutionStat> aggResult = IntStream.of(exe2Tasks).mapToObj(i -> {
            ExecutionAnalyzer.ExecutionStat stat = new ExecutionAnalyzer.ExecutionStat();
            for (int j = 0; j < i; j++) {
                stat.add(dataIterator.next().getValue());
            }
            return stat;
        }).collect(Collectors.toList());

        System.out.println("Executor stat:");
        aggResult.forEach(e -> System.out.printf("%d,%.2f\n", e.getCount(), e.getCost()));
    }

    private static int[] computeTasks(int taskCount, int exeCount) {
        int[] ret = new int[exeCount];
        Arrays.fill(ret, taskCount / exeCount);
        int k = taskCount % exeCount;
        for (int i = 0; i < k; i++) {
            ret[i]++;
        }
        return ret;
    }
}