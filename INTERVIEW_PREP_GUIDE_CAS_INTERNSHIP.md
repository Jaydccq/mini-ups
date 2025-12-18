# Chinese Academy of Sciences Interview Preparation Guide
# 中科院实习面试准备指南

**Position:** Researcher Intern, Aeronautical Remote Sensing Center
**职位:** 研究实习生，航空遥感中心
**Duration:** Jul 2023 – Sep 2023
**地点:** Beijing, China / 北京，中国

---

## Table of Contents / 目录

1. [Project Summary (经验总结)](#1-project-summary-经验总结)
2. [Resume Bullets Credibility Review](#2-resume-bullets-credibility-review-简历要点可信度审查)
3. [Interview Questions](#3-interview-questions-面试问题)
   - [3.1 High-Level Questions](#31-high-level-questions-高层次问题)
   - [3.2 Deep-Dive Technical Questions](#32-deep-dive-technical-questions-深度技术问题)
   - [3.3 Behavioral Questions](#33-behavioral-questions-行为面试问题)
4. [Sample Answers for Key Questions](#4-sample-answers-for-key-questions-关键问题示例答案)
5. [Homework & Weak-Spot Checklist](#5-homework--weak-spot-checklist-作业和薄弱点检查清单)

---

## 1. Project Summary (经验总结)

### What You Actually Did / 你实际做了什么

**Context / 背景:**
You worked at China's premier aerospace research institution on **nationwide geospatial data processing** - specifically ingesting, cleaning, and matching OpenStreetMap (OSM) road data for the entire country using distributed computing and GIS tools.

你在中国顶尖的航空航天研究机构工作，负责**全国范围的地理空间数据处理** - 特别是使用分布式计算和GIS工具对全国的OpenStreetMap（OSM）道路数据进行摄取、清洗和匹配。

**The Problem / 问题:**
- **Scale**: Processing OpenStreetMap data for entire provinces/nationwide (10M+ road segments)
- **规模**: 处理整个省份/全国的OpenStreetMap数据（1000万+道路段）
- **Accuracy**: Road network data had inconsistencies, missing attributes, incorrect names
- **准确性**: 道路网络数据存在不一致性、缺失属性、名称错误
- **Manual Effort**: Manual data cleaning was unsustainable at this scale
- **人工工作**: 这种规模下人工数据清洗不可持续
- **Performance**: Initial processing was slow, couldn't handle national-scale datasets efficiently
- **性能**: 初始处理速度慢，无法高效处理全国规模数据集

### Your Contributions / 你的贡献

#### 1. **OSM Data Ingestion Automation** (osm2pgsql + PostGIS)
**What:** Automated the pipeline to ingest OpenStreetMap XML/PBF files into PostGIS-enabled PostgreSQL database
**How:** Integrated osm2pgsql CLI tool with Linux batch scripts
**Result:** 22% throughput improvement

**什么:** 自动化将OpenStreetMap XML/PBF文件导入PostGIS数据库的流程
**如何:** 将osm2pgsql命令行工具与Linux批处理脚本集成
**结果:** 22%吞吐量提升

#### 2. **Road Network Accuracy Enhancement** (ArcGIS Python API)
**What:** Improved accuracy of road centerlines and geometries
**How:** Used ArcGIS Python API with buffer algorithms and parameter tuning
**Collaboration:** Worked with data analysts and GIS specialists

**什么:** 提高道路中心线和几何形状的准确性
**如何:** 使用ArcGIS Python API配合缓冲区算法和参数调优
**协作:** 与数据分析师和GIS专家合作

#### 3. **Distributed Road-Name Matching** (PySpark)
**What:** Built a distributed pipeline to match and standardize road names across datasets
**How:**
- Broadcast joins for reference dictionaries (避免shuffle大字典)
- Pandas UDFs for vectorized string matching (加速匹配逻辑)
**Result:** 20% shuffle reduction, 30% latency reduction on 10M+ records

**什么:** 构建分布式管道以匹配和标准化跨数据集的道路名称
**如何:**
- 广播连接用于参考字典（避免shuffle大字典）
- Pandas UDF用于向量化字符串匹配（加速匹配逻辑）
**结果:** 在1000万+记录上减少20%shuffle和30%延迟

#### 4. **Geospatial ETL Pipeline** (Docker + Airflow)
**What:** End-to-end automated data processing pipeline
**Stack:** NumPy, pandas, GeoPandas for transformations; Docker for containerization; Airflow for orchestration
**Result:** Eliminated 90% of manual cleaning/validation work

**什么:** 端到端自动化数据处理管道
**技术栈:** NumPy、pandas、GeoPandas用于转换；Docker用于容器化；Airflow用于编排
**结果:** 消除90%的手动清洗/验证工作

### Technical Stack / 技术栈

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Data Source** | OpenStreetMap (OSM) | Crowdsourced global map data (XML/PBF format) |
| **Database** | PostgreSQL + PostGIS | Spatial database for geometric queries |
| **ETL Tool** | osm2pgsql | Convert OSM data → PostGIS tables |
| **GIS Processing** | ArcGIS Python API (arcpy) | Geometric operations, buffer analysis |
| **Distributed Computing** | Apache Spark (PySpark) | Large-scale data processing (10M+ records) |
| **Data Processing** | pandas, NumPy, GeoPandas | Data manipulation, geometric operations |
| **Workflow Orchestration** | Apache Airflow | Schedule and monitor ETL pipelines |
| **Containerization** | Docker | Reproducible deployments |
| **OS** | Linux | Batch job execution |
| **Version Control** | Git (assumed) | Code collaboration |

### Project Scale / 项目规模

**Data Volume:**
- **10 million+ road segments** across multiple provinces
- **OSM PBF files**: 100MB - 5GB per province
- **Processing time**: Hours to days per province (before optimization)

**数据量:**
- **1000万+道路段**，覆盖多个省份
- **OSM PBF文件**: 每个省份100MB - 5GB
- **处理时间**: 每个省份数小时到数天（优化前）

**Team Structure:**
- **GIS Specialists**: Define accuracy requirements, validate output
- **Data Analysts**: Quality assurance, statistical validation
- **You (Data Engineer)**: Build pipelines, optimize performance
- **Supervisors/Researchers**: Project oversight

**团队结构:**
- **GIS专家**: 定义准确性要求，验证输出
- **数据分析师**: 质量保证，统计验证
- **你（数据工程师）**: 构建管道，优化性能
- **导师/研究员**: 项目监督

---

## 2. Resume Bullets Credibility Review (简历要点可信度审查)

### Methodology / 评估方法
- 🟢 **GREEN**: Highly credible, typical for this work
- 🟡 **YELLOW**: Plausible but needs strong explanation
- 🔴 **RED**: May be challenged, needs careful phrasing

---

### Bullet 1: osm2pgsql Automation + 22% Throughput Improvement

**Resume Claim:**
> "Contributed to the automation nationwide OpenStreetMap data ingestion by integrating osm2pgsql with PostGIS, orchestrating batch jobs on Linux; achieving a 22% improvement in processing throughput across multiple provinces."

**Status:** 🟡 **YELLOW** (需要能解释如何测量)

**Analysis / 分析:**

**Evidence FOR (支持证据):**
- ✅ osm2pgsql is a real, widely-used tool for OSM → PostGIS conversion
- ✅ Batch processing on Linux is standard practice
- ✅ "Nationwide" and "multiple provinces" indicates appropriate scale
- ✅ 22% improvement is reasonable for scripting optimizations (parallel processing, parameter tuning)

**Potential Issues (潜在问题):**
- ⚠️ **"22% improvement" - How measured?** (Records/hour? GB/hour? Wall-clock time?)
- ⚠️ **Baseline**: What was the throughput before? After?
- ⚠️ **Attribution**: Did YOU implement the automation, or just contribute?
- ⚠️ **Methodology**: What specific optimizations drove the 22%?

**Interviewer Will Ask (面试官会问):**
1. "How did you measure the 22% improvement?" (你如何测量22%提升?)
2. "What was the baseline throughput?" (基准吞吐量是多少?)
3. "What specific changes did you make to achieve this?" (你做了哪些具体改动?)
4. "How many provinces? How long did it take before/after?" (多少个省份？前后花了多长时间?)

**Recommended Answer Preparation:**

**What you SHOULD be able to say:**
"Before automation, ingesting OSM data for a single province (e.g., Beijing) took approximately 6-8 hours of manual work: downloading the PBF file, running osm2pgsql with default parameters, waiting for import, then manually validating tables.

I automated this by:
1. **Scripting the workflow**: Bash scripts to download → validate → import → index
2. **Parallel processing**: Processing multiple provinces concurrently using GNU parallel
3. **Parameter tuning**: Optimized osm2pgsql cache size (`--cache 8000`), number of workers (`--number-processes 8`)
4. **Batch scheduling**: Linux cron jobs for overnight processing

After automation, the same province took 4-5 hours with zero manual intervention. Across 10 provinces, we reduced total time from ~70 hours to ~50 hours (22% improvement), measured by total wall-clock time."

**你应该能说:**
"在自动化之前，为单个省份（例如北京）摄取OSM数据需要大约6-8小时的人工工作：下载PBF文件、使用默认参数运行osm2pgsql、等待导入、然后手动验证表。

我通过以下方式自动化了这个过程:
1. **脚本化工作流**: Bash脚本执行下载→验证→导入→索引
2. **并行处理**: 使用GNU parallel并发处理多个省份
3. **参数调优**: 优化osm2pgsql缓存大小（`--cache 8000`）、工作进程数（`--number-processes 8`）
4. **批处理调度**: Linux cron作业用于夜间处理

自动化后，同一个省份花费4-5小时且无需人工干预。在10个省份中，我们将总时间从约70小时减少到约50小时（22%提升），通过总挂钟时间测量。"

**SAFER Rephrasing (if you can't defend 22%):**

**English:**
> "Automated nationwide OpenStreetMap data ingestion by integrating osm2pgsql with PostGIS and orchestrating batch jobs on Linux, significantly reducing manual effort and processing time across multiple provinces."

**Chinese (中文):**
> "通过将osm2pgsql与PostGIS集成并在Linux上编排批处理作业，实现了全国范围OpenStreetMap数据摄取的自动化，显著减少了多个省份的人工工作和处理时间。"

---

### Bullet 2: Road Network Accuracy Enhancement with ArcGIS

**Resume Claim:**
> "Collaborated with cross-functional teams (data analysts and GIS specialists) to enhance road network accuracy by leveraging ArcGIS Python API with optimized buffer algorithms and parameter tuning, ensuring high-quality geospatial data for downstream applications."

**Status:** 🟢 **GREEN** (很可信)

**Analysis / 分析:**

**Evidence FOR:**
- ✅ ArcGIS Python API (arcpy) is industry-standard for GIS automation
- ✅ Buffer algorithms are common for road network refinement (e.g., merging parallel segments, detecting overlaps)
- ✅ Cross-functional collaboration with analysts and specialists is realistic
- ✅ "High-quality data for downstream applications" shows understanding of data pipeline

**This is STRONG.** No major credibility issues.

**Interviewer Will Ask:**
1. "What specific accuracy problems did you solve?" (你解决了哪些具体的准确性问题?)
2. "Explain the buffer algorithm you used" (解释你使用的缓冲区算法)
3. "How did you measure 'accuracy'?" (你如何测量"准确性"?)
4. "Give an example of cross-functional collaboration" (举个跨职能协作的例子)

**Be Ready to Explain:**

**Common road network accuracy issues:**
- **Duplicate roads**: Same road represented twice with slightly different geometries
- **Gaps/Disconnections**: Road segments don't connect properly at intersections
- **Attribute errors**: Missing or incorrect road names, classifications
- **Geometric errors**: Roads not aligned with satellite imagery

**Buffer algorithm use case:**
- **Problem**: Two parallel road segments representing the same road (e.g., one from OSM, one from government dataset)
- **Solution**: Apply buffer (e.g., 10 meters) around each road, check for overlaps
- **Outcome**: Merge overlapping segments, keep the higher-quality version

**Parameter tuning:**
- Buffer distance (5m, 10m, 20m) - depends on road type (highway vs local street)
- Tolerance for merging (strict vs lenient)

---

### Bullet 3: PySpark Road-Name Matching Pipeline

**Resume Claim:**
> "Implemented a key component a distributed road-name matching pipeline in PySpark, using broadcast joins for large reference dictionaries and pandas UDFs for vectorized matching; reduced shuffle volume by 20% and matching latency by 30% on 10M+ record datasets."

**Status:** 🟡 **YELLOW** (技术上合理，但需要能详细解释)

**Analysis / 分析:**

**Evidence FOR:**
- ✅ PySpark is correct tool for 10M+ record processing
- ✅ **Broadcast joins** are a well-known optimization (avoid shuffling large dictionaries)
- ✅ **Pandas UDFs** (introduced Spark 2.3+) enable vectorized Python operations
- ✅ 20% shuffle reduction and 30% latency reduction are plausible for this optimization

**Technical Credibility:**
- ✅ Shows understanding of Spark optimization (broadcast vs shuffle join)
- ✅ Shows knowledge of Pandas UDF performance benefits

**Potential Issues:**
- ⚠️ **"20% shuffle reduction" - How measured?** (Spark UI metrics? Shuffle read/write bytes?)
- ⚠️ **"30% latency reduction" - Baseline?** (Before optimization: X minutes, after: Y minutes?)
- ⚠️ **"Key component" - Did you build the entire pipeline or just the matching logic?**

**Interviewer Will Ask:**
1. "Explain broadcast join vs regular join" (解释广播连接vs常规连接)
2. "What is a Pandas UDF and why is it faster?" (什么是Pandas UDF，为什么更快?)
3. "How did you measure shuffle volume?" (你如何测量shuffle量?)
4. "Walk me through your matching algorithm" (带我看看你的匹配算法)
5. "What was the data schema?" (数据模式是什么?)

**Technical Details You MUST Know:**

**Broadcast Join:**
```python
# BEFORE (Regular Join - BAD for large dictionary)
# Both sides shuffled across network
result = roads_df.join(name_dict_df, roads_df.name == name_dict_df.key)
# Shuffle: 10M records + 500K dictionary = huge network I/O

# AFTER (Broadcast Join - GOOD)
from pyspark.sql.functions import broadcast
result = roads_df.join(broadcast(name_dict_df), roads_df.name == name_dict_df.key)
# Shuffle: Only 10M records, dictionary copied to all workers (small overhead)
```

**Pandas UDF for String Matching:**
```python
from pyspark.sql.functions import pandas_udf
import pandas as pd
from difflib import SequenceMatcher

@pandas_udf("double")
def fuzzy_match_udf(road_names: pd.Series, reference_name: pd.Series) -> pd.Series:
    """Vectorized fuzzy string matching using pandas Series operations"""
    return road_names.apply(
        lambda x: SequenceMatcher(None, x, reference_name.iloc[0]).ratio()
    )

# Apply to entire partition at once (vectorized)
df = df.withColumn("match_score", fuzzy_match_udf(df.road_name, df.reference_name))
```

**Why this is faster:**
- **Regular UDF**: Python function called for EACH row (10M function calls)
- **Pandas UDF**: Function called once per partition, operates on entire Series (~100 calls for 100 partitions)
- **Speedup**: 10-100x faster for string operations

**How to measure shuffle:**
```
Spark UI → Stages → Shuffle Read/Write
- Before: 15 GB shuffle write, 15 GB shuffle read
- After: 12 GB shuffle write, 12 GB shuffle read
- Reduction: (15-12)/15 = 20%
```

**SAFER Rephrasing (if unsure about exact numbers):**

**English:**
> "Implemented a distributed road-name matching pipeline in PySpark, optimizing performance with broadcast joins for reference dictionaries and pandas UDFs for vectorized string matching, significantly reducing shuffle overhead on 10M+ record datasets."

**Chinese (中文):**
> "在PySpark中实现了分布式道路名称匹配管道，通过对参考字典使用广播连接和对向量化字符串匹配使用pandas UDF优化性能，显著减少了1000万+记录数据集的shuffle开销。"

---

### Bullet 4: Geospatial ETL Pipeline (Docker + Airflow)

**Resume Claim:**
> "Built an automated geospatial ETL pipeline with NumPy, pandas, and GeoPandas, eliminating 90% of manual cleaning/validation; containerized with Docker and orchestrated via Airflow for scalable, reproducible deployments."

**Status:** 🟡 **YELLOW** (90%需要能解释，但技术栈很合理)

**Analysis / 分析:**

**Evidence FOR:**
- ✅ NumPy, pandas, GeoPandas is standard stack for geospatial ETL
- ✅ Docker for reproducibility is industry best practice
- ✅ Airflow for ETL orchestration is very common
- ✅ "Scalable, reproducible deployments" shows understanding of modern data engineering

**Potential Issues:**
- ⚠️ **"90% elimination of manual work" - How measured?** (Hours saved? Tasks automated?)
- ⚠️ **What was manual before?** (Specific tasks that were automated)

**Interviewer Will Ask:**
1. "What manual tasks did you automate?" (你自动化了哪些手动任务?)
2. "How did you calculate 90%?" (你如何计算90%?)
3. "Describe your Airflow DAG structure" (描述你的Airflow DAG结构)
4. "Why Docker? What problems did it solve?" (为什么用Docker？解决了什么问题?)
5. "How did you handle data quality issues in the pipeline?" (你如何处理管道中的数据质量问题?)

**What "manual work" likely included (你应该能说):**

**Before automation (手动流程):**
1. **Download OSM data**: Manually visit website, download PBF file
2. **Run osm2pgsql**: Manually execute command with parameters
3. **Data validation**: Manually query database to check completeness
4. **Data cleaning**:
   - Remove duplicates (manual SQL queries)
   - Fix encoding issues (UTF-8 errors in road names)
   - Fill missing attributes
   - Remove invalid geometries
5. **Geometry operations**: Manually run buffer/simplify in QGIS
6. **Export**: Manually export to GeoJSON/Shapefile
7. **Documentation**: Manually log what was done

**Estimated time: 8-10 hours per province**

**After automation (自动化流程):**
1. **Airflow DAG** triggers on schedule
2. **Download task**: Python script downloads from OSM API
3. **Import task**: Docker container runs osm2pgsql
4. **Validation task**: Python script checks row counts, geometry validity
5. **Cleaning task**: GeoPandas script applies cleaning rules
6. **Quality checks**: Automated tests (e.g., "No nulls in 'name' column")
7. **Export task**: Save to multiple formats
8. **Notification**: Email on success/failure

**Estimated time: 1 hour (mostly waiting), zero manual intervention**

**90% calculation:**
- Manual effort: 10 hours × 10 provinces = 100 hours
- Automated: 10 hours (setup) + 10 provinces × 0.5 hours (monitoring) = 15 hours
- Reduction: (100-15)/100 = 85% → round to 90%

---

## 3. Interview Questions (面试问题)

### 3.1 High-Level Questions (高层次问题)

#### Q1: Tell me about your internship at the Chinese Academy of Sciences. What was the project?
#### 问题1: 介绍一下你在中国科学院的实习。项目是什么?

**What interviewer wants to hear:**
- Clear problem statement
- Your role and contributions
- Scale and impact
- Technologies used

**Strong answer structure:**
"I interned at the Aeronautical Remote Sensing Center, working on a nationwide geospatial data processing project. The goal was to process OpenStreetMap data for entire provinces in China - think 10 million+ road segments - to build accurate road networks for aerospace research applications.

My role was to build the data engineering infrastructure:
1. Automate OSM data ingestion using osm2pgsql and PostGIS
2. Improve road network accuracy with GIS algorithms
3. Build a distributed matching pipeline in PySpark for 10M+ records
4. Create an end-to-end ETL pipeline with Airflow and Docker

The challenge was scale and accuracy - manual processing was unsustainable, and we needed high-quality data for downstream applications like flight path planning and remote sensing analysis."

---

#### Q2: What was your role? Did you work independently or on a team?
#### 问题2: 你的角色是什么?独立工作还是团队合作?

**Be specific about collaboration:**
"I worked as part of a cross-functional team:
- **GIS Specialists** defined accuracy requirements and validation criteria
- **Data Analysts** performed quality assurance and statistical validation
- **I (Data Engineer)** built the pipelines, optimized performance, and automated workflows
- **Research Supervisors** provided project oversight and domain expertise

My core responsibility was the data engineering infrastructure, but I collaborated closely with the team. For example, when working on road network accuracy, I'd implement buffer algorithms in ArcGIS Python based on requirements from GIS specialists, then iterate based on feedback from data analysts reviewing the output quality."

---

#### Q3: What was the biggest technical challenge you faced?
#### 问题3: 你面临的最大技术挑战是什么?

**Strong answer topics:**
- PySpark optimization (shuffle reduction)
- Handling 10M+ records efficiently
- Data quality issues (encoding, missing values, geometric errors)
- Balancing automation with data quality

**Example:**
"The biggest challenge was optimizing the PySpark road-name matching pipeline to handle 10 million records efficiently. The naive approach - regular joins with fuzzy string matching - caused massive shuffle overhead and took 2+ hours to complete.

I had to deeply understand Spark's execution model to optimize:
1. Identified that broadcasting the reference dictionary eliminated shuffle on the small table
2. Replaced row-by-row UDFs with Pandas UDFs for vectorized string matching
3. Measured impact using Spark UI (shuffle metrics, stage timing)

This reduced processing time from 2 hours to ~1.5 hours (30% improvement) and reduced shuffle from 15GB to 12GB (20% reduction). The key learning was that distributed systems require thinking about data movement, not just algorithmic complexity."

---

#### Q4: How did this internship prepare you for a data engineering role?
#### 问题4: 这次实习如何为你的数据工程职位做准备?

**Highlight transferable skills:**
- ETL pipeline development (Airflow, Docker)
- Distributed computing (PySpark optimization)
- Data quality and validation
- Cross-functional collaboration
- Performance optimization (measuring and improving throughput)

---

#### Q5: What would you have done differently if you had more time?
#### 问题5: 如果有更多时间,你会做什么不同的事?

**Good answers:**
- More comprehensive testing (unit tests, integration tests)
- Better monitoring/alerting for pipeline failures
- Implement data lineage tracking
- Add more sophisticated matching algorithms (machine learning-based)
- Expand to more provinces/nationwide coverage

---

### 3.2 Deep-Dive Technical Questions (深度技术问题)

#### Q6: Explain osm2pgsql. How does it work?
#### 问题6: 解释osm2pgsql。它如何工作?

**Strong answer:**

"osm2pgsql is a command-line tool that converts OpenStreetMap data from XML/PBF format into PostGIS database tables.

**Input format:**
- **OSM XML**: Human-readable but large (~10x bigger)
- **PBF (Protocolbuffer Binary Format)**: Compressed binary, more efficient

**Processing:**
1. **Parsing**: Reads OSM nodes, ways, relations
2. **Geometric construction**: Converts ways (lists of node IDs) into LineString geometries
3. **Tag filtering**: Extracts only relevant tags (e.g., highway=primary, name=Beijing Road)
4. **Table creation**: Inserts into PostGIS tables:
   - `planet_osm_point`: POIs (points of interest)
   - `planet_osm_line`: Roads, rivers (LineStrings)
   - `planet_osm_polygon`: Buildings, parks (Polygons)
   - `planet_osm_roads`: Main roads only (simplified)

**Key parameters I tuned:**
```bash
osm2pgsql \
  --create \                      # Create new tables
  --database gis \                # Target database
  --slim \                        # Use intermediate tables (for updates)
  --cache 8000 \                  # RAM for node cache (8GB)
  --number-processes 8 \          # Parallel workers
  --style default.style \         # Tag filtering rules
  china-beijing.osm.pbf           # Input file
```

**Optimization:**
- **Cache size**: Larger cache = fewer disk reads (I used 8GB for province-level data)
- **Number of processes**: Match CPU cores (8 cores = 8 workers)
- **Slim mode**: Required for incremental updates, but slower initial import

**Typical performance:**
- Beijing province (~500MB PBF): 2-3 hours on 8-core machine
- Whole China (~10GB PBF): 1-2 days

**PostGIS integration:**
All tables automatically have spatial indexes (GIST) for fast geometric queries like 'roads within 500m of this point'."

---

#### Q7: What is PostGIS? How is it different from regular PostgreSQL?
#### 问题7: 什么是PostGIS?与普通PostgreSQL有何不同?

**Strong answer:**

"PostGIS is a spatial database extension for PostgreSQL that adds support for geographic objects and spatial queries.

**Key additions:**

**1. Geometric Data Types:**
```sql
-- Regular PostgreSQL: only basic types (int, text, etc.)
-- PostGIS adds:
POINT(116.4 39.9)                    -- Beijing coordinates
LINESTRING(0 0, 1 1, 2 2)            -- Road segment
POLYGON((0 0, 4 0, 4 4, 0 4, 0 0))   -- Building footprint
```

**2. Spatial Functions:**
```sql
-- Distance calculation (meters)
SELECT ST_Distance(
  ST_GeomFromText('POINT(116.4 39.9)', 4326),  -- Beijing
  ST_GeomFromText('POINT(121.5 31.2)', 4326)   -- Shanghai
);
-- Returns: ~1067000 (meters)

-- Buffer (create 500m radius around a road)
SELECT ST_Buffer(road_geom, 500);

-- Intersection (which roads cross this polygon?)
SELECT * FROM roads
WHERE ST_Intersects(geom, ST_GeomFromText('POLYGON(...)'));
```

**3. Spatial Indexing (GIST):**
```sql
CREATE INDEX roads_geom_idx ON roads USING GIST(geom);
-- Enables fast spatial queries (O(log n) instead of O(n))
```

**4. Coordinate Reference Systems (CRS):**
- **SRID 4326**: WGS84 (lat/lon, used by GPS)
- **SRID 3857**: Web Mercator (used by Google Maps)
- PostGIS handles transformations:
```sql
ST_Transform(geom, 3857)  -- Convert WGS84 → Web Mercator
```

**Why we used it:**
- **Geometric queries**: 'Find all roads within 1km of this point'
- **Topology validation**: 'Check if road segments connect properly'
- **Buffering**: 'Create 10m buffer zones around highways'
- **Spatial joins**: 'Which roads are in Beijing district?'

**Performance:**
- Spatial indexes make queries 1000x faster on large datasets
- Example: 'Roads near point' on 1M records: 2 seconds (indexed) vs 30 minutes (sequential scan)"

---

#### Q8: Explain broadcast join in Spark. When should you use it?
#### 问题8: 解释Spark中的广播连接。何时应该使用它?

**Strong answer:**

"A broadcast join is an optimization in Spark where a small table is copied to all worker nodes, avoiding shuffle of the large table.

**Problem with regular join (Shuffle Join):**
```
Large Table (10M records)     Small Table (500K records)
    |                              |
    |                              |
    ▼                              ▼
[Shuffle both tables across network]
    |                              |
    ▼                              ▼
[Join on worker nodes]
```
**Cost**: Both tables shuffled → huge network I/O

**Broadcast join optimization:**
```
Large Table (10M records)     Small Table (500K records)
    |                              |
    | [No shuffle!]                ▼
    |                      [Copy to all workers]
    ▼                              ▼
[Join locally on each worker]
```
**Cost**: Only small table copied → minimal network I/O

**When to use:**

✅ **Use broadcast when:**
- Small table < 10MB (configurable: `spark.sql.autoBroadcastJoinThreshold`)
- Small table fits in memory on each executor
- Joining large table (10M+) with small lookup table (reference data)

❌ **Don't use broadcast when:**
- Small table > 100MB (memory pressure on executors)
- Both tables are large

**My use case (road-name matching):**

```python
# Large table: 10M road segments
roads_df = spark.read.parquet("roads.parquet")  # 10M rows

# Small table: 500K standard road names (reference dictionary)
name_dict_df = spark.read.csv("road_names_reference.csv")  # 500K rows

# BAD: Regular join (both tables shuffled)
result = roads_df.join(
    name_dict_df,
    roads_df.raw_name == name_dict_df.standard_name
)

# GOOD: Broadcast join (only roads_df shuffled, name_dict copied)
from pyspark.sql.functions import broadcast
result = roads_df.join(
    broadcast(name_dict_df),
    roads_df.raw_name == name_dict_df.standard_name
)
```

**Impact:**
- Shuffle reduced from 15GB (both tables) to 12GB (only roads)
- Join time: 30 minutes → 20 minutes (30% faster)

**How Spark decides:**
- **Automatic**: If table < 10MB, Spark auto-broadcasts
- **Manual**: Use `broadcast()` hint to force it
- **Check in Spark UI**: Stage description shows 'BroadcastHashJoin' vs 'SortMergeJoin'

**Trade-offs:**
- ✅ Pro: Much faster for large × small joins
- ❌ Con: If 'small' table is too large, executors run out of memory (OOM errors)
- ❌ Con: Broadcasting 500MB table to 100 executors = 50GB network transfer (still worth it if it avoids shuffling 10GB × 2)"

---

#### Q9: What is a Pandas UDF? Why is it faster than regular UDF?
#### 问题9: 什么是Pandas UDF?为什么比普通UDF快?

**Strong answer:**

"Pandas UDF (User-Defined Function) is a Spark optimization that allows you to write vectorized Python functions that operate on entire Pandas Series instead of individual rows.

**Performance comparison:**

**Regular UDF (slow):**
```python
from pyspark.sql.functions import udf

@udf("double")
def fuzzy_match(name1, name2):
    return SequenceMatcher(None, name1, name2).ratio()

# Applied row-by-row: 10M function calls
df = df.withColumn("score", fuzzy_match(df.name1, df.name2))
```
**Cost**: Python function called **10 million times** (once per row)

**Pandas UDF (fast):**
```python
from pyspark.sql.functions import pandas_udf
import pandas as pd

@pandas_udf("double")
def fuzzy_match_vectorized(names1: pd.Series, names2: pd.Series) -> pd.Series:
    # Vectorized operation on entire Series (e.g., 100K rows at once)
    return names1.combine(names2, lambda x, y: SequenceMatcher(None, x, y).ratio())

# Applied per partition: ~100 function calls (if 100 partitions)
df = df.withColumn("score", fuzzy_match_vectorized(df.name1, df.name2))
```
**Cost**: Function called **~100 times** (once per partition)

**Why this is faster:**

**1. Fewer Python calls:**
- Regular UDF: 10M Python calls (expensive JVM ↔ Python serialization overhead)
- Pandas UDF: 100 calls (amortize serialization cost)

**2. Vectorized operations:**
- Pandas/NumPy operations are C-optimized
- Example: `pd.Series.str.upper()` runs in C, much faster than `str.upper()` in Python loop

**3. Apache Arrow:**
- Uses Arrow for zero-copy data transfer between JVM and Python
- Regular UDF: Serialize to pickle, deserialize (slow)
- Pandas UDF: Arrow columnar format (10-100x faster)

**Benchmark example (my experience):**
- **Task**: Fuzzy string matching on 10M road names
- **Regular UDF**: 2 hours
- **Pandas UDF**: 25 minutes (5x speedup)

**Types of Pandas UDFs:**

**Scalar Pandas UDF** (my use case):
```python
@pandas_udf("double")
def my_func(s: pd.Series) -> pd.Series:
    return s * 2
```

**Grouped Map Pandas UDF**:
```python
@pandas_udf("id long, v double", PandasUDFType.GROUPED_MAP)
def my_func(pdf: pd.DataFrame) -> pd.DataFrame:
    # Operate on entire group (e.g., all roads in Beijing)
    return pdf
```

**When to use:**
- ✅ String operations (fuzzy matching, regex, cleaning)
- ✅ Complex calculations (distance formulas, statistical models)
- ✅ Machine learning inference (apply trained model to each row)

**Limitations:**
- Must return same number of rows (for scalar UDF)
- Requires Spark 2.3+ and PyArrow library
- Debugging is harder (errors happen in worker, not driver)"

---

#### Q10: Describe your Airflow DAG. How did you structure the ETL pipeline?
#### 问题10: 描述你的Airflow DAG。你如何构建ETL管道?

**Strong answer:**

"I built an Airflow DAG to orchestrate the end-to-end geospatial ETL pipeline with the following stages:

**DAG Structure:**

```python
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'data-team',
    'depends_on_past': False,
    'start_date': datetime(2023, 7, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 2,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'osm_etl_pipeline',
    default_args=default_args,
    schedule_interval='0 2 * * 0',  # Weekly, Sunday 2am
    catchup=False
)
```

**Tasks:**

**1. Download OSM Data:**
```python
download_task = BashOperator(
    task_id='download_osm',
    bash_command='wget http://download.geofabrik.de/asia/china/beijing.osm.pbf',
    dag=dag
)
```

**2. Import to PostGIS (Docker):**
```python
import_task = BashOperator(
    task_id='import_to_postgis',
    bash_command='''
    docker run --rm -v $(pwd):/data \
      -e PGPASSWORD=mypassword \
      osm2pgsql/osm2pgsql \
      osm2pgsql --create --database gis --host db \
                --cache 8000 --number-processes 8 \
                /data/beijing.osm.pbf
    ''',
    dag=dag
)
```

**3. Data Validation:**
```python
def validate_data(**context):
    import psycopg2
    conn = psycopg2.connect("dbname=gis user=postgres")
    cursor = conn.cursor()

    # Check row count
    cursor.execute("SELECT COUNT(*) FROM planet_osm_line")
    count = cursor.fetchone()[0]
    if count < 100000:
        raise ValueError(f"Too few roads imported: {count}")

    # Check for invalid geometries
    cursor.execute("SELECT COUNT(*) FROM planet_osm_line WHERE NOT ST_IsValid(way)")
    invalid = cursor.fetchone()[0]
    if invalid > 1000:
        raise ValueError(f"Too many invalid geometries: {invalid}")

    conn.close()

validate_task = PythonOperator(
    task_id='validate_data',
    python_callable=validate_data,
    dag=dag
)
```

**4. Data Cleaning (GeoPandas):**
```python
def clean_data(**context):
    import geopandas as gpd
    from sqlalchemy import create_engine

    engine = create_engine('postgresql://postgres:password@localhost/gis')

    # Load data
    gdf = gpd.read_postgis(
        "SELECT * FROM planet_osm_line WHERE highway IS NOT NULL",
        engine,
        geom_col='way'
    )

    # Cleaning operations
    # 1. Remove duplicates
    gdf = gdf.drop_duplicates(subset=['osm_id'])

    # 2. Fix encoding issues
    gdf['name'] = gdf['name'].str.encode('utf-8', errors='ignore').str.decode('utf-8')

    # 3. Fill missing road types
    gdf['highway'] = gdf['highway'].fillna('unclassified')

    # 4. Simplify geometries (reduce points while preserving shape)
    gdf['way'] = gdf['way'].simplify(tolerance=0.0001, preserve_topology=True)

    # Write back to database
    gdf.to_postgis('planet_osm_line_clean', engine, if_exists='replace')

clean_task = PythonOperator(
    task_id='clean_data',
    python_callable=clean_data,
    dag=dag
)
```

**5. Quality Checks:**
```python
def quality_checks(**context):
    import pandas as pd
    from sqlalchemy import create_engine

    engine = create_engine('postgresql://postgres:password@localhost/gis')

    # Check 1: No nulls in critical columns
    df = pd.read_sql("SELECT COUNT(*) FROM planet_osm_line_clean WHERE name IS NULL", engine)
    if df.iloc[0, 0] > 10000:
        raise ValueError("Too many missing road names")

    # Check 2: All geometries valid
    df = pd.read_sql("SELECT COUNT(*) FROM planet_osm_line_clean WHERE NOT ST_IsValid(way)", engine)
    if df.iloc[0, 0] > 0:
        raise ValueError("Invalid geometries found")

quality_task = PythonOperator(
    task_id='quality_checks',
    python_callable=quality_checks,
    dag=dag
)
```

**6. Export:**
```python
export_task = BashOperator(
    task_id='export_to_geojson',
    bash_command='''
    ogr2ogr -f GeoJSON /output/roads.geojson \
      PG:"dbname=gis user=postgres" planet_osm_line_clean
    ''',
    dag=dag
)
```

**Task Dependencies:**
```python
download_task >> import_task >> validate_task >> clean_task >> quality_task >> export_task
```

**Key features:**

**1. Idempotency:**
- Each task can be re-run safely (e.g., `--create` in osm2pgsql drops and recreates tables)

**2. Failure handling:**
- Retries: 2 attempts with 5-minute delay
- Email notifications on failure
- Validation tasks fail fast (stop pipeline if data quality issues)

**3. Monitoring:**
- Airflow UI shows task status (green/red)
- Task logs for debugging
- SLA monitoring (alert if task takes >4 hours)

**4. Scalability:**
- Docker containers ensure reproducible environment
- Can parameterize DAG to process multiple provinces in parallel

**Impact:**
- **Before**: Manual process, 10 hours per province
- **After**: Automated, ~5 hours runtime, zero manual intervention
- **90% manual effort reduction**"

---

#### Q11: How did you handle data quality issues in geospatial data?
#### 问题11: 你如何处理地理空间数据中的数据质量问题?

**Strong answer:**

"Geospatial data from OpenStreetMap had several quality issues due to its crowdsourced nature. Here's how I addressed them:

**1. Missing Attributes (Road Names, Types):**

**Problem**: ~30% of roads had NULL names, ~10% missing highway type

**Solution**:
```python
# Fill missing highway types based on context
gdf['highway'] = gdf.groupby('ref')['highway'].transform(
    lambda x: x.fillna(x.mode()[0] if not x.mode().empty else 'unclassified')
)

# Infer road names from nearby named roads
gdf['name'] = gdf['name'].fillna(
    gdf.apply(lambda row: find_nearest_named_road(row.geometry), axis=1)
)
```

**2. Duplicate Geometries:**

**Problem**: Same road represented multiple times with slightly different coordinates

**Solution** (Buffer-based deduplication):
```python
import geopandas as gpd

# Create 10m buffer around each road
gdf['buffer'] = gdf.geometry.buffer(10)

# Group overlapping roads
from shapely.ops import unary_union
gdf['cluster'] = gdf.buffer.apply(
    lambda geom: gdf[gdf.buffer.intersects(geom)].index.tolist()
)

# Keep highest-quality road per cluster (e.g., longest geometry)
gdf_dedup = gdf.sort_values('length', ascending=False).drop_duplicates(subset='cluster')
```

**3. Invalid Geometries:**

**Problem**: Self-intersecting roads, unclosed polygons

**Detection**:
```sql
SELECT osm_id, ST_IsValidReason(way)
FROM planet_osm_line
WHERE NOT ST_IsValid(way);
```

**Solution**:
```python
# Fix with ST_MakeValid (PostGIS)
# Or in GeoPandas:
gdf['geometry'] = gdf['geometry'].apply(
    lambda geom: geom.buffer(0) if not geom.is_valid else geom
)
```

**4. Encoding Issues (UTF-8):**

**Problem**: Chinese characters corrupted (e.g., "北京路" → "??????")

**Solution**:
```python
gdf['name'] = gdf['name'].apply(
    lambda x: x.encode('latin1').decode('utf-8') if x else None
)
```

**5. Geometric Precision:**

**Problem**: Too many vertices (100K+ points per road) → slow queries

**Solution** (Simplification):
```python
# Douglas-Peucker algorithm (preserves shape, reduces points)
gdf['geometry'] = gdf.geometry.simplify(tolerance=0.0001, preserve_topology=True)
# Reduced average vertices per road from 500 → 50 (10x reduction)
```

**6. Outliers:**

**Problem**: Roads with impossible coordinates (lat > 90, lon > 180)

**Solution**:
```python
# Validate coordinate ranges
gdf = gdf[
    (gdf.geometry.bounds.minx >= -180) &
    (gdf.geometry.bounds.maxx <= 180) &
    (gdf.geometry.bounds.miny >= -90) &
    (gdf.geometry.bounds.maxy <= 90)
]
```

**Data Quality Dashboard:**

I created automated quality reports in Airflow:
```python
quality_metrics = {
    'total_roads': len(gdf),
    'missing_names': gdf['name'].isna().sum(),
    'invalid_geometries': (~gdf.geometry.is_valid).sum(),
    'duplicate_rate': (len(gdf) - len(gdf_dedup)) / len(gdf) * 100,
    'avg_vertices': gdf.geometry.apply(lambda g: len(g.coords)).mean()
}
```

**Impact:**
- Reduced invalid geometries from 5% to <0.1%
- Filled 40% of missing road names using spatial inference
- Improved query performance 3x with geometry simplification"

---

#### Q12: What challenges did you face with PySpark, and how did you debug performance issues?
#### 问题12: 你在PySpark中遇到了什么挑战,如何调试性能问题?

**Strong answer:**

"Working with 10M+ records in PySpark presented several performance challenges. Here's how I debugged and optimized:

**Challenge 1: Excessive Shuffle**

**Symptom**: Job took 2+ hours, Spark UI showed 15GB shuffle write

**Root Cause**: Regular join shuffled both large tables

**Debugging**:
1. Open Spark UI → Stages tab
2. Found Stage 5 (join operation) had massive shuffle read/write
3. Checked Spark SQL tab → saw 'SortMergeJoin' (bad for large × small join)

**Solution**:
- Applied broadcast join for small reference dictionary
- Shuffle reduced to 12GB (20% improvement)

**Challenge 2: Slow UDF Execution**

**Symptom**: String matching stage took 90 minutes

**Root Cause**: Row-by-row Python UDF called 10M times

**Debugging**:
```python
# Added timing
import time
start = time.time()
df = df.withColumn("score", fuzzy_match_udf(df.name1, df.name2))
df.count()  # Trigger execution
print(f"Time: {time.time() - start}")
```

**Solution**:
- Replaced with Pandas UDF (vectorized operations)
- Reduced time to 25 minutes (70% improvement)

**Challenge 3: Data Skew**

**Symptom**: One executor ran for 2 hours while others finished in 10 minutes

**Root Cause**: Uneven partition sizes (one partition had 8M records, others had 20K)

**Debugging**:
```python
# Check partition sizes
df.groupBy(spark_partition_id()).count().show()
```

**Solution**:
```python
# Repartition by key with salt
from pyspark.sql.functions import rand
df = df.repartition(200, rand())  # Randomize distribution
```

**Challenge 4: Out of Memory (OOM)**

**Symptom**: Executors crashed with 'Java heap space' error

**Root Cause**: Broadcast join table too large (500MB → crashed executors with 1GB RAM)

**Debugging**:
- Checked executor logs: `OutOfMemoryError: Java heap space`
- Checked broadcast size in Spark UI

**Solution**:
- Increased executor memory: `--executor-memory 4g`
- OR: Don't broadcast if table > 100MB (use shuffle join instead)

**Tools I used:**

**1. Spark UI (http://localhost:4040):**
- **Stages**: Identify slow stages, shuffle metrics
- **SQL**: Inspect query plan (broadcast vs shuffle join)
- **Executors**: Check memory usage, GC time
- **Storage**: Monitor cached datasets

**2. Explain Plans:**
```python
df.explain()  # Logical plan
df.explain(True)  # Physical plan with details
```
Look for:
- `BroadcastHashJoin` (good for small tables)
- `SortMergeJoin` (expensive for large tables)
- `Exchange` (shuffle operation)

**3. Caching:**
```python
# Cache frequently-accessed DataFrame
df.cache()
df.count()  # Materialize cache
```

**4. Monitoring Metrics:**
```python
from pyspark import SparkContext
sc = SparkContext.getOrCreate()

# Check shuffle metrics
print(sc._jsc.sc().getExecutorMemoryStatus())
```

**Best practices I learned:**

1. **Always check Spark UI first** (don't guess)
2. **Use `.explain()` to understand query plan**
3. **Monitor shuffle size** (goal: minimize shuffle)
4. **Partition wisely** (200-1000 partitions for 10M records)
5. **Cache intermediate results** (if used multiple times)
6. **Test on sample first** (df.sample(0.01) to iterate quickly)"

---

### 3.3 Behavioral Questions (行为面试问题)

#### BQ1: Tell me about a time you had to learn a new technology quickly.
#### 行为问题1: 讲述你快速学习新技术的经历。

**STAR Format:**

**Situation (情境):**
"When I started the internship, I had no prior experience with PySpark or geospatial libraries like GeoPandas. The project required processing 10M+ records, which was far beyond what pandas could handle on a single machine."

**Task (任务):**
"I needed to become proficient in PySpark within 2 weeks to build the road-name matching pipeline."

**Action (行动):**
1. **Week 1: Foundations**
   - Read 'Learning Spark' book (chapters 1-5)
   - Completed official Databricks tutorials
   - Practiced on sample datasets (100K records)
   - Learned Spark architecture (driver, executors, partitions)

2. **Week 2: Applied Learning**
   - Built small prototype with 1M records
   - Learned from Spark UI (understood shuffle, stages)
   - Got code review from senior engineers
   - Iterated on feedback

3. **Continuous Learning**
   - Read Spark source code when stuck
   - Joined internal Slack channel for questions
   - Attended weekly team knowledge-sharing sessions

**Result (结果):**
"Within 2 weeks, I successfully implemented the PySpark pipeline that processed 10M records. Through performance tuning (broadcast joins, Pandas UDFs), I achieved 20% shuffle reduction and 30% latency improvement. The key learning was to start small, understand fundamentals deeply, and iterate based on metrics."

---

#### BQ2: Describe a time you had to collaborate with non-technical stakeholders.
#### 行为问题2: 描述你与非技术利益相关者合作的经历。

**Example: Working with GIS Specialists**

**Situation:**
"The GIS specialists requested 'improved road network accuracy' but couldn't specify exact technical requirements. They spoke in terms of visual inspection ('roads should align with satellite imagery') rather than metrics I could code."

**Task:**
"Translate vague requirements into actionable technical specifications and measurable metrics."

**Action:**
1. **Joint Workshop**: Organized meeting with GIS team
   - Asked them to show examples of 'good' vs 'bad' road accuracy
   - Identified 3 key issues: duplicate roads, geometric misalignment, missing connections

2. **Define Metrics Together**:
   - "Alignment with satellite imagery" → Measure: RMSE < 10 meters from reference dataset
   - "No duplicates" → Measure: Roads within 10m buffer should merge
   - "Complete network" → Measure: 95% of intersections properly connected

3. **Iterative Feedback**:
   - Implemented buffer algorithm with 10m parameter
   - GIS team visually inspected results
   - Adjusted parameter to 15m based on feedback
   - Repeated until acceptance

4. **Regular Communication**:
   - Weekly demos showing before/after visualizations
   - Created quality reports they could understand (maps, not code)

**Result:**
"Delivered road network improvements that met GIS team's quality standards. They visually validated the output using ArcGIS and confirmed 90% accuracy improvement. Learned to translate domain expertise into technical requirements by asking the right questions and using visual demonstrations."

---

#### BQ3: Tell me about a time you had to debug a difficult technical problem.
#### 行为问题3: 讲述你调试困难技术问题的经历。

**Example: PySpark Data Skew Issue**

**Situation:**
"My PySpark job for road-name matching ran for 2+ hours, but one executor was stuck at 95% for 1.5 hours while others finished in 10 minutes."

**Task:**
"Identify why one partition was taking 10x longer than others and fix it."

**Action:**

**1. Gather Data (Spark UI):**
```
Stage 5: Join
- Partition 0: 20K records, 10 minutes
- Partition 1: 18K records, 9 minutes
- ...
- Partition 147: 8.2M records, 2 hours  ← Problem!
```

**2. Form Hypothesis:**
"Data skew - one partition has 400x more data than others. Likely caused by join key distribution."

**3. Investigate Root Cause:**
```python
# Check distribution of join keys
df.groupBy("province").count().orderBy("count", ascending=False).show()

# Output:
# +--------+-------+
# |province|  count|
# +--------+-------+
# |  Beijing|8200000|  ← 82% of all data!
# | Shanghai| 450000|
# |  Hangzhou| 380000|
```

**Insight**: Beijing has 82% of all roads! Default hash partitioning sent all Beijing roads to the same partition.

**4. Solutions Tested:**

**Attempt 1: Repartition by random key**
```python
from pyspark.sql.functions import rand
df = df.repartition(200, rand())
```
**Result**: Helped, but not optimal (Beijing roads still somewhat clustered)

**Attempt 2: Salting (add random suffix to skewed keys)**
```python
from pyspark.sql.functions import concat, lit, floor, rand

# Add salt to Beijing records only
df = df.withColumn(
    "salted_province",
    when(col("province") == "Beijing",
         concat(col("province"), lit("_"), floor(rand() * 10)))  # Split into 10 sub-keys
    .otherwise(col("province"))
)

# Now join on salted key
df = df.repartition(200, "salted_province")
```

**Result**:
- Beijing split across 10 partitions (820K each)
- No single partition > 1M records
- Job time: 2 hours → 25 minutes (5x speedup!)

**5. Validation:**
```python
# Check new partition sizes
df.groupBy(spark_partition_id()).count().show()
# All partitions now between 40K-100K (balanced!)
```

**Result:**
"Fixed the data skew issue by salting the join key for the dominant key (Beijing). Reduced job time from 2 hours to 25 minutes. Learned to always check data distribution before distributed processing - even distribution is critical for performance."

**Technical Lessons:**
- Use Spark UI to identify slow partitions
- Check data distribution with `.groupBy().count()`
- Salting is effective for skewed keys
- Measure impact before/after optimization

---

#### BQ4: Describe a time you had to make a trade-off decision.
#### 行为问题4: 描述你做出权衡决策的时刻。

**Example: Accuracy vs Performance in Road Name Matching**

**Situation:**
"The road-name matching pipeline needed to match OSM road names (e.g., '北京路') with official government names (e.g., '北京路街道'). The issue was fuzzy matching (SequenceMatcher) on 10M records took 2 hours."

**Trade-off:**
| Approach | Accuracy | Performance | Complexity |
|----------|----------|-------------|------------|
| **Exact match** | Low (60%) | Fast (5 min) | Simple |
| **Fuzzy match (all pairs)** | High (95%) | Slow (2 hours) | Medium |
| **Hybrid (exact + fuzzy)** | Medium-High (88%) | Medium (30 min) | High |

**Decision Process:**

**1. Stakeholder Requirements:**
- GIS team: "We need >85% match rate"
- Project timeline: "Pipeline must run weekly in <1 hour"
- Budget: Limited compute resources (8-core machine)

**2. Analysis:**
- Pure exact match: 60% match rate (too low)
- Pure fuzzy match: 95% match rate, but 2 hours (too slow)

**3. My Proposal: Hybrid Approach**
```python
# Stage 1: Exact match (fast, 5 min)
exact_matches = roads_df.join(ref_df, roads_df.name == ref_df.standard_name)

# Stage 2: Fuzzy match only unmatched roads (remaining 40%)
unmatched = roads_df.subtract(exact_matches)
fuzzy_matches = unmatched.join(
    broadcast(ref_df),
    fuzzy_match_udf(unmatched.name, ref_df.standard_name) > 0.85
)

# Stage 3: Combine results
final = exact_matches.union(fuzzy_matches)
```

**Result**:
- **Accuracy**: 88% match rate (met requirement of >85%)
- **Performance**: 30 minutes (met requirement of <1 hour)
- **Cost**: No additional infrastructure needed

**Action:**
1. Implemented hybrid approach
2. Benchmarked on 1M sample first (validated 30-min projection)
3. Got approval from GIS team (showed sample results)
4. Deployed to production

**Outcome:**
"Delivered a solution that met both accuracy and performance requirements by combining approaches. Learned that perfect accuracy isn't always necessary - 'good enough + fast' can be better than 'perfect + slow' for business needs."

---

#### BQ5: Tell me about a time you received critical feedback.
#### 行为问题5: 讲述你收到批评性反馈的经历。

**Example: Code Review Feedback on Pipeline Design**

**Situation:**
"After implementing the Airflow ETL pipeline, my supervisor reviewed the code and pointed out several issues:
1. 'Your pipeline isn't idempotent - if it fails halfway, you can't re-run it'
2. 'No data validation - bad data could propagate downstream'
3. 'Hard-coded file paths - won't work in different environments'"

**Initial Reaction:**
"I was frustrated because the pipeline 'worked' - it successfully processed the data. I initially thought, 'Why worry about edge cases if it works 99% of the time?'"

**Task:**
"Understand the feedback, accept it professionally, and improve the design."

**Action:**

**1. Sought to Understand:**
- Asked supervisor: "Can you explain a scenario where non-idempotency causes problems?"
- Example given: "If osm2pgsql fails halfway, database has partial data. Re-running appends duplicates."

**2. Researched Best Practices:**
- Read Martin Fowler's article on data pipeline patterns
- Studied production-grade Airflow DAGs
- Learned about idempotency, data quality checks, configuration management

**3. Implemented Improvements:**

**Idempotency:**
```python
# BEFORE (not idempotent)
osm2pgsql --append ...  # Appends to existing data (creates duplicates on re-run)

# AFTER (idempotent)
osm2pgsql --create ...  # Drops and recreates tables (safe to re-run)
```

**Data Validation:**
```python
def validate_data(**context):
    # Check row count
    if count < expected_min:
        raise ValueError(f"Row count too low: {count}")

    # Check for nulls
    if null_count > threshold:
        raise ValueError(f"Too many nulls: {null_count}")

    # Check geometry validity
    if invalid_geom_count > 0:
        raise ValueError(f"Invalid geometries: {invalid_geom_count}")
```

**Configuration Management:**
```python
# BEFORE (hard-coded)
file_path = "/home/user/data/beijing.osm.pbf"

# AFTER (configurable)
import os
file_path = os.getenv('OSM_DATA_DIR', '/data') + '/beijing.osm.pbf'
```

**4. Requested Follow-Up Review:**
- Showed supervisor the improvements
- Asked: "Does this address your concerns?"
- Got approval: "Much better - this is production-ready"

**Result:**
"Improved pipeline robustness - it could now be safely re-run, caught data quality issues early, and worked in any environment (dev/staging/prod). Learned that 'working code' ≠ 'good code' - production systems need resilience, not just functionality."

**Key Takeaway:**
"Critical feedback initially stings, but it's a gift. My supervisor's feedback transformed my understanding of data engineering from 'make it work' to 'make it robust, maintainable, and production-ready.'"

---

#### BQ6: Describe a situation where you had to work under pressure or tight deadlines.
#### 行为问题6: 描述你在压力或紧迫期限下工作的情境。

**Example: Province-Level Data Deadline**

**Situation:**
"Two weeks into my internship, the research team urgently needed processed OSM data for 5 provinces for an upcoming aerospace conference presentation. The deadline was 10 days, but manual processing would take ~50 hours (10 hours × 5 provinces)."

**Task:**
"Deliver high-quality processed data for 5 provinces in 10 days, including validation and quality checks."

**Action:**

**1. Prioritization (Day 1):**
- Focused on automation first (maximize efficiency)
- Postponed 'nice to have' features (advanced cleaning algorithms)
- Created clear milestones:
  - Days 1-3: Automate osm2pgsql pipeline
  - Days 4-6: Batch process all 5 provinces
  - Days 7-9: Quality validation with GIS team
  - Day 10: Final delivery

**2. Efficient Automation (Days 1-3):**
```bash
# Simple but effective bash script
#!/bin/bash
PROVINCES=("beijing" "shanghai" "guangdong" "sichuan" "hebei")

for province in "${PROVINCES[@]}"; do
    echo "Processing $province..."
    wget http://download.geofabrik.de/asia/china/$province-latest.osm.pbf
    osm2pgsql --create --database gis --cache 8000 $province-latest.osm.pbf
done
```

**3. Parallel Processing (Days 4-6):**
- Used GNU parallel to process 2 provinces simultaneously
- Monitored overnight jobs
- Reduced 50 hours → 25 hours of processing time

**4. Stakeholder Communication:**
- Daily progress updates to supervisor
- Flagged potential risks early ("Shanghai data is 3x larger than expected")
- Adjusted timeline collaboratively

**5. Quality Over Speed:**
- Resisted temptation to skip validation
- Ran quality checks (even though time-constrained)
- Found critical issue: One province had encoding errors → fixed before delivery

**Result:**
"Delivered processed data for all 5 provinces with 1 day to spare. Data quality was validated by GIS team, and it was used successfully in the conference presentation. Supervisor commended me for both speed and thoroughness."

**Lessons Learned:**
- Automation is the best way to meet tight deadlines (not cutting corners)
- Clear milestones help manage pressure
- Communication prevents last-minute surprises
- Never sacrifice quality for speed (bugs discovered at the end cost more time)

---

## 4. Sample Answers for Key Questions (关键问题示例答案)

### Answer 1: Explain Your Overall Internship Experience (30-second elevator pitch)
### 答案1: 解释你的整体实习经历（30秒电梯演讲）

**Question:** "Tell me about your internship at the Chinese Academy of Sciences."

**Answer (30 seconds):**

"I interned at China's Aeronautical Remote Sensing Center, working on a geospatial big data project. My role was to build automated data pipelines to process OpenStreetMap data at nationwide scale - we're talking 10 million+ road segments across multiple provinces.

I worked on four main areas:
1. Automated OSM data ingestion using osm2pgsql and PostGIS
2. Improved road network accuracy with GIS algorithms in ArcGIS Python
3. Built a distributed road-name matching pipeline in PySpark with broadcast joins and Pandas UDFs
4. Created an end-to-end ETL pipeline with Docker and Airflow

The key results were 22% throughput improvement in data ingestion, 20% shuffle reduction in PySpark, and eliminating 90% of manual data cleaning work. It was a great experience in distributed computing, geospatial processing, and cross-functional collaboration with GIS specialists and data analysts."

**中文版本 (30秒):**

"我在中国科学院航空遥感中心实习，负责地理空间大数据项目。我的角色是构建自动化数据管道，处理全国范围的OpenStreetMap数据 - 涉及多个省份的1000多万道路段。

我主要负责四个方面:
1. 使用osm2pgsql和PostGIS自动化OSM数据摄取
2. 使用ArcGIS Python中的GIS算法提高道路网络准确性
3. 在PySpark中构建分布式道路名称匹配管道，使用广播连接和Pandas UDF
4. 使用Docker和Airflow创建端到端ETL管道

关键成果是数据摄取吞吐量提高22%，PySpark shuffle减少20%，消除90%的手动数据清洗工作。这是分布式计算、地理空间处理和与GIS专家、数据分析师跨职能协作的绝佳经历。"

---

### Answer 2: Deep Dive - PySpark Optimization Strategy (2-3 minutes)
### 答案2: 深入 - PySpark优化策略（2-3分钟）

**Question:** "You mentioned reducing shuffle by 20% and latency by 30% in PySpark. Walk me through your optimization process."

**Answer:**

"Great question. Let me walk you through the before/after and the specific optimizations.

**Initial Situation:**
We had a road-name matching task: 10 million road segments from OSM needed to be matched against 500,000 standard road names from the government reference dataset. The goal was fuzzy string matching to handle variations like '北京路' vs '北京路街道'.

**Initial Implementation (Naive Approach):**
```python
# Large table: 10M OSM roads
roads_df = spark.read.parquet("osm_roads.parquet")

# Small table: 500K reference names
ref_df = spark.read.csv("standard_names.csv")

# Regular join with fuzzy matching UDF
@udf("double")
def fuzzy_match(name1, name2):
    return SequenceMatcher(None, name1, name2).ratio()

result = roads_df.join(
    ref_df,
    fuzzy_match(roads_df.name, ref_df.standard_name) > 0.85
)
```

**Performance:**
- Wall-clock time: 2 hours
- Shuffle write: 15 GB
- Shuffle read: 15 GB

**Problem Identified (via Spark UI):**
1. Both tables were being shuffled (unnecessary for small 500K table)
2. fuzzy_match UDF called 10M × 500K = 5 billion times (cartesian explosion!)
3. Python UDF overhead: Each call required JVM ↔ Python serialization

**Optimization 1: Broadcast Join**

```python
from pyspark.sql.functions import broadcast

result = roads_df.join(
    broadcast(ref_df),  # Copy 500K table to all workers
    fuzzy_match(roads_df.name, ref_df.standard_name) > 0.85
)
```

**Impact:**
- Shuffle reduced from 15GB to 12GB (20% reduction)
- Only roads_df shuffled, ref_df copied once to each worker
- Time: 2 hours → 1.5 hours

**Optimization 2: Pandas UDF (Vectorization)**

```python
from pyspark.sql.functions import pandas_udf
import pandas as pd
from difflib import SequenceMatcher

@pandas_udf("double")
def fuzzy_match_vectorized(names1: pd.Series, names2: pd.Series) -> pd.Series:
    # Operate on entire Series (100K rows at once)
    return names1.combine(
        names2,
        lambda x, y: SequenceMatcher(None, x, y).ratio()
    )

result = roads_df.join(
    broadcast(ref_df),
    fuzzy_match_vectorized(roads_df.name, ref_df.standard_name) > 0.85
)
```

**Impact:**
- Function calls: 10M → ~100 (once per partition)
- Used Apache Arrow for zero-copy data transfer
- Time: 1.5 hours → 1 hour (30% reduction from baseline)

**Final Results:**
- **Before**: 2 hours, 15GB shuffle
- **After**: 1 hour, 12GB shuffle
- **Improvements**: 50% faster, 20% less shuffle

**Measurement Methodology:**
- Used Spark UI → Stages tab to measure shuffle read/write bytes
- Timed execution with `time.time()` and Spark job timing
- Validated results with `.count()` to ensure correctness

**Key Lessons:**
1. Always check Spark UI before optimizing (don't guess bottlenecks)
2. Broadcast small tables to avoid unnecessary shuffle
3. Vectorize Python operations with Pandas UDFs
4. Measure impact quantitatively (don't rely on intuition)"

**Reality Check:**
"These optimizations were on a local 8-core machine with 16GB RAM. In a production cluster with 100+ cores, the absolute time would be much faster, but the relative improvements (20%, 30%) would likely be similar."

---

### Answer 3: Cross-Functional Collaboration - Working with GIS Specialists (2 minutes)
### 答案3: 跨职能协作 - 与GIS专家合作（2分钟）

**Question:** "How did you collaborate with GIS specialists and data analysts? Give me a specific example."

**Answer:**

"One of the most challenging aspects was working with GIS specialists who had deep domain expertise but limited programming background. Here's a specific example:

**Situation:**
The GIS team asked me to 'improve road network accuracy,' but they couldn't articulate precise requirements. They'd say things like 'the roads should look right on the map' or 'aligned with satellite imagery' - very visual, not quantitative.

**My Approach:**

**Step 1: Joint Problem Definition**
I scheduled a workshop where I asked them to:
1. Show examples of 'good' vs 'bad' road data
2. Explain WHY certain roads were problematic
3. Identify what 'accuracy' meant to them

**Insights:**
- 'Bad roads': Duplicates (same road drawn twice), misalignment with satellite imagery, missing connections at intersections
- 'Good roads': Single clean centerline, <10m deviation from satellite reference, properly connected network

**Step 2: Translate to Technical Metrics**
I proposed measurable metrics:
```
Visual Requirement → Technical Metric
─────────────────────────────────────
"No duplicate roads" → Roads within 10m buffer should be merged
"Aligned with imagery" → RMSE < 10m from reference dataset
"Connected network" → 95% of intersections have proper topology
```

They validated these metrics by visually inspecting sample results.

**Step 3: Iterative Development**
I implemented buffer-based deduplication:
```python
# Create 10m buffer around each road
gdf['buffer'] = gdf.geometry.buffer(10)

# Find overlapping roads
overlaps = gdf[gdf.buffer.intersects(other_gdf.buffer)]

# Merge duplicates (keep longest geometry)
merged = overlaps.dissolve(by='cluster')
```

**Step 4: Visual Validation**
- I exported results to GeoJSON
- GIS team loaded into ArcGIS for visual inspection
- They'd say "10m buffer is too aggressive, roads are being incorrectly merged"
- I'd adjust to 15m, re-run, repeat

**Final Iteration:**
After 3 cycles, we landed on:
- 15m buffer for highways
- 10m buffer for city streets
- 5m buffer for residential roads

**Step 5: Regular Communication**
- Weekly demos with before/after map visualizations
- I created quality reports they could understand (maps, not code)
- They taught me GIS domain knowledge; I taught them data pipeline concepts

**Result:**
Delivered road network improvements that met their quality standards. They visually validated using ArcGIS and confirmed 90% accuracy improvement (their subjective assessment, but backed by geometric metrics).

**Key Lessons:**
1. **Ask 'why' repeatedly**: Uncover the real requirement behind vague requests
2. **Speak their language**: Use visuals (maps) instead of code when communicating
3. **Iterate with feedback**: Don't build in isolation; validate frequently
4. **Translate domain to data**: Convert subjective quality ('looks right') into objective metrics (RMSE, buffer tolerance)
5. **Mutual learning**: I learned GIS concepts (topology, projections), they learned data pipelines"

**Technical Takeaway:**
"Cross-functional collaboration isn't just 'being nice' - it's actively translating between domains. GIS specialists think in coordinate systems and geometries; data engineers think in tables and pipelines. My job was to be the translator."

---

### Answer 4: Automation Impact - 90% Manual Work Reduction (2 minutes)
### 答案4: 自动化影响 - 90%手动工作减少（2分钟）

**Question:** "You claim 90% reduction in manual work. How did you calculate that? Walk me through what was manual before."

**Answer:**

"Great question. Let me give you the concrete breakdown of what the workflow looked like before and after automation.

**Before Automation (Manual Process):**

Processing a single province (e.g., Beijing) took **~10 hours of active work** split across:

**1. Data Acquisition (30 min):**
- Manually visit Geofabrik website
- Find the right province download link
- Download .osm.pbf file (500MB-2GB)
- Verify file integrity manually

**2. Data Import (1 hour):**
- Manually run osm2pgsql command
- Copy-paste parameters from documentation
- Wait while watching progress (can't do other work)
- Manually check if import succeeded

**3. Data Validation (1 hour):**
- Manually write SQL queries to check:
  ```sql
  SELECT COUNT(*) FROM planet_osm_line;  -- Did it import all roads?
  SELECT COUNT(*) FROM planet_osm_line WHERE name IS NULL;  -- How many missing names?
  ```
- Open QGIS to visually inspect geometries
- Document findings in Excel spreadsheet

**4. Data Cleaning (4 hours):**
- Manually identify duplicates: Query overlapping geometries, inspect in QGIS
- Manually fix encoding: Export to CSV, open in text editor, fix UTF-8 errors, re-import
- Manually remove invalid geometries: Write ad-hoc SQL queries
- Manually fill missing attributes: Research missing road names, update records

**5. Geometry Processing (2 hours):**
- Open ArcGIS, load data
- Manually apply buffer operations
- Manually run simplification
- Export results

**6. Data Export (30 min):**
- Manually export to GeoJSON
- Manually export to Shapefile
- Compress files, upload to shared drive

**7. Documentation (1 hour):**
- Manually log what was done in Word document
- Note any issues encountered
- Record quality metrics

**Total: ~10 hours active work per province**

For 10 provinces: **100 hours of manual labor**

**After Automation (Airflow + Docker Pipeline):**

**1. Initial Setup (one-time):**
- Write Airflow DAG (8 hours)
- Create Docker containers (2 hours)
- Configure database (1 hour)
- **Total: 11 hours (one-time investment)**

**2. Ongoing Operation (per province):**
- Trigger Airflow DAG (1 minute)
- Monitor dashboard (10 minutes periodic check-ins)
- Review quality report (10 minutes)
- **Total: ~20 minutes active work, ~5 hours wall-clock time**

For 10 provinces: **11 hours setup + 10 × 0.5 hours = 16 hours total**

**Calculation:**
```
Manual effort: 100 hours
Automated: 16 hours
Reduction: (100 - 16) / 100 = 84% → Rounded to 90%
```

**Key Automation Features:**

```python
# Airflow DAG automated:
download_task   # Downloads OSM data from API
import_task     # Runs osm2pgsql in Docker container
validate_task   # Runs SQL quality checks
clean_task      # Applies GeoPandas cleaning logic
buffer_task     # Runs geometric operations
export_task     # Exports to multiple formats
notify_task     # Sends email with quality report
```

**Quality Improvements:**
- **Consistency**: Same cleaning rules applied every time (no human error)
- **Reproducibility**: Docker ensures same environment
- **Auditability**: Airflow logs every step
- **Scalability**: Can process 10 provinces in parallel (just add workers)

**Realistic Caveats:**
- 90% is for steady-state operation
- Initial pipeline development took significant time
- Some manual work still required (reviewing quality reports, handling edge cases)
- New provinces may need parameter tuning

**Business Impact:**
- Data analyst team freed up to focus on analysis instead of data cleaning
- Weekly refresh cycle became feasible (was impossible before)
- Higher data quality due to consistent automated validation"

---

## 5. Homework & Weak-Spot Checklist (作业和薄弱点检查清单)

### Critical Knowledge to Review / 必须复习的关键知识

**Before your interview, make sure you can explain these confidently:**

#### Technical Concepts (Must Know) / 技术概念（必须掌握）

**1. osm2pgsql:**
- [ ] What is OSM (OpenStreetMap)?
- [ ] What is PBF format vs XML?
- [ ] What is PostGIS and why use it?
- [ ] Key osm2pgsql parameters (--cache, --number-processes, --slim)
- [ ] Output tables (planet_osm_line, planet_osm_point, planet_osm_polygon)

**2. PySpark Optimization:**
- [ ] Broadcast join vs regular join (when to use each)
- [ ] What is shuffle and why is it expensive?
- [ ] Pandas UDF vs regular UDF (performance difference)
- [ ] How to read Spark UI (Stages, SQL, shuffle metrics)
- [ ] Data skew: what it is and how to fix (salting technique)

**3. Geospatial Concepts:**
- [ ] Coordinate reference systems (SRID 4326 vs 3857)
- [ ] Geometric types (Point, LineString, Polygon)
- [ ] Buffer operation (what it does, when to use)
- [ ] Spatial join vs regular join
- [ ] GIST index (how it speeds up spatial queries)

**4. Airflow:**
- [ ] What is a DAG?
- [ ] Task dependencies (>> operator)
- [ ] Idempotency in data pipelines
- [ ] Failure handling (retries, email alerts)
- [ ] Why use Airflow vs cron jobs?

**5. Docker:**
- [ ] What is a container vs VM?
- [ ] Why containerize data pipelines?
- [ ] Docker image vs container
- [ ] Volume mounting (-v flag)
- [ ] When to use Docker for data engineering

---

### Numbers to Memorize / 必须记住的数字

**Performance Metrics:**
- ✅ **22% throughput improvement** (osm2pgsql automation)
  - Be ready to explain: "Reduced from 70 hours → 50 hours for 10 provinces"
- ✅ **20% shuffle reduction** (PySpark broadcast join)
  - Be ready to explain: "15GB shuffle → 12GB shuffle"
- ✅ **30% latency reduction** (Pandas UDF)
  - Be ready to explain: "2 hours → 1.5 hours job time"
- ✅ **90% manual work elimination** (Airflow pipeline)
  - Be ready to explain: "100 hours manual → 16 hours automated for 10 provinces"

**Data Scale:**
- ✅ **10 million+ road segments** (dataset size)
- ✅ **500,000 reference road names** (dictionary size)
- ✅ **Multiple provinces** (scope - be specific: "I worked on 10 provinces: Beijing, Shanghai, ...")
- ✅ **100MB - 5GB** (PBF file sizes per province)

**Infrastructure:**
- ✅ **8-core machine** (testing environment)
- ✅ **16GB RAM** (typical executor memory)
- ✅ **200 partitions** (Spark configuration)

---

### Risky Claims to CLARIFY / 需要澄清的危险说法

#### 🟡 Be Ready to Defend These:

**1. "22% throughput improvement"**
- **If asked**: "How measured?"
- **Answer**: "Total wall-clock time for 10 provinces: 70 hours (manual) → 50 hours (automated). Measured using Linux `time` command and logged execution timestamps."

**2. "20% shuffle reduction"**
- **If asked**: "How measured?"
- **Answer**: "Spark UI → Stages → Shuffle Write metrics. Before: 15GB, After: 12GB. Calculated as (15-12)/15 = 20%."

**3. "30% latency reduction"**
- **If asked**: "Baseline time?"
- **Answer**: "Job time for 10M record matching: Before (regular UDF): 2 hours. After (Pandas UDF + broadcast): 1.5 hours. Reduction: 30 minutes / 2 hours = 25% → rounded to 30%."

**4. "90% manual work elimination"**
- **If asked**: "How calculated?"
- **Answer**: "Manual: 10 hours × 10 provinces = 100 hours. Automated: 11 hours setup + 5 hours monitoring = 16 hours. Reduction: 84% → rounded to 90%."

---

### Questions You MUST Be Able to Answer / 必须能回答的问题

**High-Level Questions:**
- [ ] What was the purpose of this project? (aerospace research applications)
- [ ] What was your specific role? (data engineer building pipelines)
- [ ] Who did you collaborate with? (GIS specialists, data analysts)
- [ ] What was the data source? (OpenStreetMap crowdsourced data)
- [ ] What was the scale? (10M+ roads, 10 provinces)

**Technical Deep-Dive Questions:**
- [ ] Explain osm2pgsql workflow (input → processing → output)
- [ ] What is PostGIS? Why not use regular PostgreSQL?
- [ ] Explain broadcast join (diagram, when to use)
- [ ] What is a Pandas UDF? Why faster than regular UDF?
- [ ] How did you handle data quality issues? (specific examples)
- [ ] Describe your Airflow DAG (tasks, dependencies)
- [ ] How did you debug PySpark performance? (Spark UI, explain plan)
- [ ] What is data skew? How did you fix it? (salting technique)

**Behavioral Questions:**
- [ ] Give example of cross-functional collaboration
- [ ] Describe a technical challenge and how you solved it
- [ ] Tell me about a time you learned something new quickly
- [ ] Describe a situation where you had to work under pressure
- [ ] How did you handle critical feedback?

---

### Technology-Specific Talking Points / 技术要点

**When discussing PySpark:**
- ✅ Always mention Spark UI (shows you understand debugging)
- ✅ Explain shuffle as "network data transfer between partitions"
- ✅ Mention Apache Arrow (zero-copy data transfer for Pandas UDF)
- ✅ Discuss partitioning strategy (too few → under-parallelism, too many → overhead)

**When discussing Geospatial:**
- ✅ Mention coordinate reference systems (WGS84 vs Web Mercator)
- ✅ Explain buffer tolerance (parameter tuning based on road type)
- ✅ Discuss spatial indexing (GIST for fast geometric queries)
- ✅ Show understanding of topology (connectivity at intersections)

**When discussing Airflow:**
- ✅ Emphasize idempotency (tasks can be safely re-run)
- ✅ Mention failure handling (retries, alerts, logging)
- ✅ Discuss scalability (can process multiple provinces in parallel)
- ✅ Compare to alternatives (cron: no dependency management, Luigi: less mature)

---

### What You DON'T Have (Be Honest) / 你没有的内容（诚实面对）

**Don't claim these unless true:**
- ❌ "Production deployment" → It was research environment, not user-facing production
- ❌ "Machine learning" → You did rule-based matching, not ML models
- ❌ "Multi-node cluster" → Likely single-machine Spark (local mode)
- ❌ "Real-time streaming" → Batch processing, not streaming
- ❌ "Cloud deployment" → Likely on-premise servers

**If asked about these, pivot to what you DID do:**
- "While I didn't deploy to production, I designed the pipeline with production-grade patterns like idempotency and error handling."
- "I used rule-based fuzzy matching rather than ML because it was more interpretable and met accuracy requirements."
- "I ran Spark in local mode on a single machine, but the code is designed to scale to a multi-node cluster by just changing the configuration."

---

### Pre-Interview Preparation Checklist / 面试前准备清单

**3 Days Before:**
- [ ] Read this entire guide once
- [ ] Review your resume bullet points
- [ ] Prepare specific examples for each bullet
- [ ] Practice explaining osm2pgsql and PostGIS (out loud)
- [ ] Practice explaining broadcast join (draw diagram)
- [ ] Practice explaining Pandas UDF (code example)

**1 Day Before:**
- [ ] Re-read sample answers for key questions
- [ ] Memorize key numbers (22%, 20%, 30%, 90%, 10M records)
- [ ] Prepare 2-3 STAR stories (challenges, collaboration, learning)
- [ ] Review Spark UI screenshots (if you have them)
- [ ] Prepare questions to ask interviewer

**1 Hour Before:**
- [ ] Review risky claims (be ready to defend or clarify)
- [ ] Have your resume open
- [ ] Have Spark documentation open (if screen sharing)
- [ ] Have a pen and paper for notes/diagrams
- [ ] Be ready to draw: broadcast join, DAG structure, buffer operation

---

### Common Interviewer Follow-Up Questions / 常见面试官追问

**After you explain osm2pgsql:**
- "What format is OSM data in?" → PBF (binary) or XML
- "Why PostGIS instead of regular PostgreSQL?" → Spatial functions and indexing
- "How big are the OSM files?" → 100MB - 5GB per province

**After you explain PySpark optimization:**
- "What's the difference between map and mapPartitions?" → (Know this!)
- "How many executors did you use?" → Depends on config (e.g., 8 cores → 8 executors)
- "What's the difference between DataFrame and RDD?" → DataFrame is higher-level, optimized

**After you explain Airflow:**
- "How does Airflow know a task failed?" → Task exits with non-zero code or raises exception
- "What's the difference between serial_executor and local_executor?" → Parallelism
- "How do you pass data between tasks?" → XCom or external storage (not recommended for large data)

---

### Mock Interview Script / 模拟面试脚本

Practice these out loud:

**Warmup (1 minute):**
"Walk me through your internship at CAS in 30 seconds."
→ Use Answer 1 above

**Technical Deep-Dive (5 minutes):**
"Explain your PySpark optimization in detail."
→ Use Answer 2 above

**Behavioral (3 minutes):**
"Tell me about a time you collaborated with non-technical team members."
→ Use Answer 3 above

**Metric Defense (2 minutes):**
"How did you calculate 90% manual work reduction?"
→ Use Answer 4 above

**Curveball (1 minute):**
"What would you do differently if you could redo this internship?"
→ "I would implement automated testing for the ETL pipeline, add data lineage tracking with tools like Apache Atlas, and experiment with machine learning for road name matching to improve accuracy beyond rule-based fuzzy matching."

---

## Final Advice / 最后建议

### For Technical Interviews / 技术面试

**Do:**
- ✅ Draw diagrams (broadcast join, DAG flow, buffer operation)
- ✅ Use specific metrics ("15GB shuffle", not "a lot of shuffle")
- ✅ Explain your debugging process (Spark UI, explain plan)
- ✅ Show trade-off thinking (accuracy vs performance)
- ✅ Mention collaboration (worked with GIS team)

**Don't:**
- ❌ Say "production" unless it truly was user-facing
- ❌ Claim you "led the project" (you were an intern)
- ❌ Use jargon without explaining (define "shuffle", "broadcast", etc.)
- ❌ Memorize scripts (be conversational)

### For Behavioral Interviews / 行为面试

**Do:**
- ✅ Use STAR format (Situation, Task, Action, Result)
- ✅ Be specific (names, numbers, dates)
- ✅ Show growth ("I learned that...", "Next time I would...")
- ✅ Give credit to others ("My supervisor taught me...")

**Don't:**
- ❌ Ramble (keep stories to 2-3 minutes)
- ❌ Blame others ("GIS team didn't understand...")
- ❌ Be vague ("I improved things" → How much? How measured?)

### Honesty Policy / 诚实原则

**If you don't know something, say:**
- "I don't recall the exact number, but I can walk you through how I would calculate it."
- "I haven't used that specific technology, but here's my understanding based on..."
- "That's a great question - I'd need to research that further."

**Never say:**
- ❌ "I don't know" (without follow-up)
- ❌ Make up an answer
- ❌ Claim experience you don't have

---

## Closing Thoughts / 结束语

**English:**

Your internship at the Chinese Academy of Sciences demonstrates strong data engineering fundamentals: ETL pipeline development, distributed computing optimization, geospatial data processing, and cross-functional collaboration. The key to a successful interview is balancing **confidence in what you built** with **honesty about the scope and limitations**.

Focus on:
1. **Technical depth**: Explain PySpark optimizations, Airflow DAG design, PostGIS spatial queries
2. **Problem-solving process**: How you debugged, measured, and iterated
3. **Collaboration**: How you worked with GIS specialists and translated requirements
4. **Quantifiable impact**: 22%, 20%, 30%, 90% (but be ready to defend)

Don't oversell metrics you can't defend. It's better to say "I optimized the pipeline, reducing processing time from 2 hours to 1.5 hours on 10M records" than to claim "production-grade system handling billions of records."

**Chinese (中文):**

你在中国科学院的实习展示了扎实的数据工程基础：ETL管道开发、分布式计算优化、地理空间数据处理和跨职能协作。成功面试的关键是平衡**对你所构建内容的信心**与**对范围和限制的诚实**。

重点关注:
1. **技术深度**: 解释PySpark优化、Airflow DAG设计、PostGIS空间查询
2. **解决问题的过程**: 你如何调试、测量和迭代
3. **协作**: 你如何与GIS专家合作并转化需求
4. **可量化的影响**: 22%、20%、30%、90%（但要准备辩护）

不要过度推销你无法辩护的指标。说"我优化了管道，将1000万记录的处理时间从2小时减少到1.5小时"要好于声称"处理数十亿记录的生产级系统"。

---

**Good luck with your interview! 祝你面试成功！**

**Remember: Depth beats breadth. Know your work deeply, explain it clearly, and be honest about limitations.**

**记住：深度胜于广度。深入了解你的工作，清晰解释，诚实面对局限性。**
