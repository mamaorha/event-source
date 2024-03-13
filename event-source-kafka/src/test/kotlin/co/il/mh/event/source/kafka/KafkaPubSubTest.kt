package co.il.mh.event.source.kafka

import co.il.mh.event.source.core.PubSubContractTest

class KafkaPubSubTest : PubSubContractTest(pubSub = IT.kafkaPubSub)