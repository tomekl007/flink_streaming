package com.tomekl007.jobs.wordcount;

import com.tomekl007.jobs.wordcount.WordCount;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class WordCountIntegrationTest {
    private final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

    @Test
    public void givenDataSet_whenExecuteWordCount_thenReturnWordCount() throws Exception {
        //given
        List<String> lines = Arrays.asList("This is a first sentence", "This is a second sentence with a one word");

        //when
        DataSet<Tuple2<String, Integer>> result = WordCount.startWordCount(env, lines);

        //then
        List<Tuple2<String, Integer>> collect = result.collect();
        assertThat(collect).containsExactlyInAnyOrder(
          new Tuple2<>("a", 3), new Tuple2<>("sentence", 2), new Tuple2<>("word", 1),
          new Tuple2<>("is", 2), new Tuple2<>("this", 2), new Tuple2<>("second", 1),
          new Tuple2<>("first", 1), new Tuple2<>("with", 1), new Tuple2<>("one", 1));
    }

    @Test
    public void givenListOfAmounts_whenUseMapReduce_thenSumAmountsThatAreOnlyAboveThreshold() throws Exception {
        //given
        DataSet<Integer> amounts = env.fromElements(1, 29, 40, 50);
        int threshold = 30;

        //when
        List<Integer> collect = amounts
          .filter(a -> a > threshold)
          .reduce((integer, t1) -> integer + t1)
          .collect();

        //then
        assertThat(collect.get(0)).isEqualTo(90);
    }

    @Test
    public void givenDataSetOfComplexObjects_whenMapToGetOneField_thenReturnedListHaveProperElements() throws Exception {
        //given
        DataSet<Person> personDataSource = env.fromCollection(Arrays.asList(new Person(23, "Tom"), new Person(75, "Michael")));

        //when
        List<Integer> ages = personDataSource.map(p -> p.age).collect();

        //then
        assertThat(ages).hasSize(2);
        assertThat(ages).contains(23, 75);

    }

    @Test
    public void givenDataSet_whenSortItByOneField_thenShouldReturnSortedDataSet() throws Exception {
        //given
        Tuple2<Integer, String> secondPerson = new Tuple2<>(4, "Tom");
        Tuple2<Integer, String> thirdPerson = new Tuple2<>(5, "Scott");
        Tuple2<Integer, String> fourthPerson = new Tuple2<>(200, "Michael");
        Tuple2<Integer, String> firstPerson = new Tuple2<>(1, "Jack");
        DataSet<Tuple2<Integer, String>> transactions = env.fromElements(fourthPerson, secondPerson,
          thirdPerson, firstPerson);


        //when
        List<Tuple2<Integer, String>> sorted = transactions
          .sortPartition(new IdKeySelectorTransaction(), Order.ASCENDING)
          .collect();

        //then
        assertThat(sorted).containsExactly(firstPerson, secondPerson, thirdPerson, fourthPerson);
    }


    @Test
    public void giveTwoDataSets_whenJoinUsingId_thenProduceJoinedData() throws Exception {
        //given
        Tuple3<Integer, String, String> address = new Tuple3<>(1, "5th Avenue", "London");
        DataSet<Tuple3<Integer, String, String>> addresses = env.fromElements(address);

        Tuple2<Integer, String> firstTransaction = new Tuple2<>(1, "Transaction_1");
        DataSet<Tuple2<Integer, String>> transactions =
          env.fromElements(firstTransaction, new Tuple2<>(12, "Transaction_2"));


        //when
        List<Tuple2<Tuple2<Integer, String>, Tuple3<Integer, String, String>>> joined =
          transactions.join(addresses)
            .where(new IdKeySelectorTransaction())
            .equalTo(new IdKeySelectorAddress())
            .collect();

        //then
        assertThat(joined).hasSize(1);
        assertThat(joined).contains(new Tuple2<>(firstTransaction, address));

    }


    private static class IdKeySelectorTransaction implements KeySelector<Tuple2<Integer, String>, Integer> {
        @Override
        public Integer getKey(Tuple2<Integer, String> value) {
            return value.f0;
        }
    }

    private static class IdKeySelectorAddress implements KeySelector<Tuple3<Integer, String, String>, Integer> {
        @Override
        public Integer getKey(Tuple3<Integer, String, String> value) {
            return value.f0;
        }
    }

    private static class Person {
        private final int age;
        private final String name;

        private Person(int age, String name) {
            this.age = age;
            this.name = name;
        }
    }

}