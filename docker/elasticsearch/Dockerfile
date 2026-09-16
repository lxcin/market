FROM elasticsearch:8.11.0

# 中文分词 IK + 拼音 analysis-pinyin（版本必须与 ES 一致）
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.11.0 \
 && bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-pinyin/8.11.0
